package app.kiddo.app

import android.app.job.JobScheduler
import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import org.koin.core.context.GlobalContext
import java.util.concurrent.TimeUnit

// Every few hours, on any network, Kiddo looks for replies and due follow-ups and says so in a notification. It only
// reads and never sends.
class InboxWork(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        GlobalContext.get().get<InboxCheck>().run(background = true)
        return Result.success()
    }

    companion object {
        private const val NAME = "inbox"
        private const val EVERY_HOURS = 4L
        private const val OLD_JOB = 1

        fun schedule(context: Context) {
            // Builds before WorkManager left a system job behind; its service is gone.
            context.getSystemService(JobScheduler::class.java)?.cancel(OLD_JOB)
            val request = PeriodicWorkRequestBuilder<InboxWork>(EVERY_HOURS, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
