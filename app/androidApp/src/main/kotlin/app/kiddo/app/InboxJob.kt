package app.kiddo.app

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

// Every few hours, on any network, Kiddo looks for replies and due follow-ups and says so in a notification. The
// system's own job scheduler runs it (no extra library); it only reads and never sends.
class InboxJob : JobService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartJob(params: JobParameters): Boolean {
        scope.launch {
            GlobalContext.get().get<InboxCheck>().run(background = true)
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        private const val JOB = 1
        private const val EVERY = 4 * 60 * 60 * 1000L

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            if (scheduler.getPendingJob(JOB) != null) return
            val job = JobInfo.Builder(JOB, ComponentName(context, InboxJob::class.java))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(EVERY)
                .setPersisted(false)
                .build()
            scheduler.schedule(job)
        }
    }
}
