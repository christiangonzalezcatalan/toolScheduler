package toolScheduler

import groovy.time.TimeCategory
import org.springframework.beans.factory.annotation.Autowired
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher

class SchedulerJob {
    static triggers = {
        simple name: 'Scheduler', repeatInterval: 1000l
        simple name: 'RefreshTools', startDelay: 180000, repeatInterval: 180000l // 3 minutos
    }

    def schedulerService

    void execute(context) {
        switch(context.trigger.name) {
            case 'Scheduler':
                schedulerService.notifyTools()
                break
            case 'RefreshTools':
                schedulerService.refreshTools()
                break;

        }
    }
}
