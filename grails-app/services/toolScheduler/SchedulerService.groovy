package toolScheduler

import grails.transaction.Transactional
import groovy.time.TimeCategory
import org.springframework.beans.factory.annotation.Autowired
import com.budjb.rabbitmq.publisher.RabbitMessagePublisher
import grails.plugins.rest.client.RestBuilder
import grails.util.Holders
import org.springframework.http.HttpStatus

@Transactional
class SchedulerService {
    static def tools
    static Date start = new Date()

    @Autowired
    RabbitMessagePublisher rabbitMessagePublisher
    RestBuilder restClient = new RestBuilder()
    String gemsbbUrl = Holders.grailsApplication.config.getProperty('injector.gemsbbUrl')

    private def getToolsFromBB() {
        def resp = restClient.get("${gemsbbUrl}/tools")

        if(resp.getStatusCode() != HttpStatus.OK) {
            throw new Exception("Error al obtener las herramientas del Blackboard. HttpStatusCode: ${resp.getStatusCode()}")
        }

        resp.json
    }

    def getTools() {
        if(tools == null) {
            tools = getToolsFromBB().collect() {
                [
                    name: it.name,
                    repeatInterval: it.repeatInterval,
                    lastExecutionTime: null
                ]
            }
        }
        tools
    }

    def refreshTools() {
        println 'Refresh tools'
        def bbTools = getToolsFromBB().collect() {
            [
                name: it.name,
                repeatInterval: it.repeatInterval,
                lastExecutionTime: null
            ]
        }

        bbTools.each() {
            def t = it
            def currentTool = tools.find() { it.name == t.name }
            if(!currentTool) {
                tools << t
            }
            else {
                currentTool.repeatInterval = t.repeatInterval
            }
        }
    }

    def notifyTools() {
        def toolsToNotify = getTools().findAll(){ it.repeatInterval != null }
        toolsToNotify.each() {
            def now = new Date()
            def last = it.lastExecutionTime ?: start

            if(TimeCategory.minus(now, last).toMilliseconds() >= it.repeatInterval * 1000) {
                it.lastExecutionTime = new Date()
                println "se ejecuta ${it.name} a las ${it.lastExecutionTime}"
                def name = it.name.toString()
                rabbitMessagePublisher.send {
                    routingKey = "${name}.load"
                    exchange = 'testGemsBBExchange'
                    body = name
                }
            }
        }
    }
}
