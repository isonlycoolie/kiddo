import BackgroundTasks
import KiddoKit
import SwiftUI
import UIKit
import UserNotifications

// The thin iOS shell: it starts the shared app and hosts its one Compose view controller full-bleed. Every screen
// lives in Kotlin; Swift adds only what iOS alone can do (CryptoKit, the app refresh task, the switcher cover).
@main
struct KiddoApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var delegate

    var body: some Scene {
        WindowGroup {
            ComposeHost().ignoresSafeArea()
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        StartIosKt.startKiddoIos(crypto: AppleCryptoBridge(), host: Shell.shared)
        UNUserNotificationCenter.current().delegate = self
        InboxTask.register()
        return true
    }

    // Kiddo's notifications show as banners even while Kiddo is open, as they do on Android.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound])
    }
}

// Erase everything asks the shell for a fresh first screen; the host controller swaps it in.
final class Shell: NSObject, ShellHost {
    static let shared = Shell()
    weak var host: HostController?

    func restart() {
        host?.reload()
    }
}

// The inbox check iOS runs now and then in the background (it decides when). Kiddo also checks on every return.
enum InboxTask {
    static let id = "app.kiddo.inbox"
    private static let every: TimeInterval = 4 * 60 * 60

    static func register() {
        BGTaskScheduler.shared.register(forTaskWithIdentifier: id, using: nil) { task in
            guard let task = task as? BGAppRefreshTask else { return }
            schedule()
            let refresh = InboxRefresh()
            task.expirationHandler = {
                refresh.cancel()
                task.setTaskCompleted(success: false)
            }
            refresh.start(done: Finished(task: task))
        }
    }

    static func schedule() {
        let request = BGAppRefreshTaskRequest(identifier: id)
        request.earliestBeginDate = Date(timeIntervalSinceNow: every)
        try? BGTaskScheduler.shared.submit(request)
    }

    private final class Finished: NSObject, RefreshDone {
        private let task: BGAppRefreshTask

        init(task: BGAppRefreshTask) {
            self.task = task
        }

        func finished(success: Bool) {
            task.setTaskCompleted(success: success)
        }
    }
}

// Compose draws edge to edge and reads the safe areas and the keyboard itself.
private struct ComposeHost: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let host = HostController()
        Shell.shared.host = host
        return host
    }

    func updateUIViewController(_ controller: UIViewController, context: Context) {}
}

// Holds the Compose controller plus one invisible scroll view: UIKit sends a status bar tap to the scroll view that
// has scrollsToTop on, and this one hands it to Kotlin, which scrolls the page on screen. It also lays a plain cover
// over Kiddo while the app switcher takes its snapshot, when the user asked for that.
final class HostController: UIViewController, UIScrollViewDelegate {
    private var compose: UIViewController?
    private let statusBarTap = UIScrollView(frame: CGRect(x: 0, y: 0, width: 1, height: 1))
    private let cover = UIView()

    override func viewDidLoad() {
        super.viewDidLoad()
        view.backgroundColor = .systemBackground
        statusBarTap.scrollsToTop = true
        statusBarTap.contentSize = CGSize(width: 1, height: 2)
        statusBarTap.contentOffset = CGPoint(x: 0, y: 1)
        statusBarTap.delegate = self
        view.addSubview(statusBarTap)
        cover.backgroundColor = .systemBackground
        cover.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        attach(MainViewControllerKt.MainViewController())

        let center = NotificationCenter.default
        center.addObserver(self, selector: #selector(leaving), name: UIApplication.willResignActiveNotification, object: nil)
        center.addObserver(self, selector: #selector(back), name: UIApplication.didBecomeActiveNotification, object: nil)
        center.addObserver(self, selector: #selector(background), name: UIApplication.didEnterBackgroundNotification, object: nil)
    }

    // A new root after Erase everything: the old screens go with the old app graph.
    func reload() {
        if let old = compose {
            old.willMove(toParent: nil)
            old.view.removeFromSuperview()
            old.removeFromParent()
        }
        attach(MainViewControllerKt.MainViewController())
    }

    private func attach(_ controller: UIViewController) {
        addChild(controller)
        controller.view.frame = view.bounds
        controller.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(controller.view)
        controller.didMove(toParent: self)
        compose = controller
    }

    @objc private func leaving() {
        guard MainViewControllerKt.hidesInSwitcher() else { return }
        cover.frame = view.bounds
        view.addSubview(cover)
    }

    @objc private func back() {
        cover.removeFromSuperview()
    }

    @objc private func background() {
        InboxTask.schedule()
    }

    func scrollViewShouldScrollToTop(_ scrollView: UIScrollView) -> Bool {
        MainViewControllerKt.onStatusBarTap()
        return false
    }
}
