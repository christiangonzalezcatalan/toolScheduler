// Place your Spring DSL code here
beans = {
    schedulerService(toolScheduler.SchedulerService) {}
    schedulerJob(toolScheduler.SchedulerJob) {
        schedulerService = ref("schedulerService")
    }
}
