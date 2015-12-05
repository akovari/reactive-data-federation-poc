import static ch.qos.logback.classic.Level.*

import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.rolling.RollingFileAppender
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy


appender("STDOUT", ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "%date{ISO8601} %-5level %logger{36} %X{sourceThread} - %msg%n"
    }

    filter(ThresholdFilter) {
        level = "DEBUG"
    }
}

appender("FILE", RollingFileAppender) {
    file = "logs/server.log"

    rollingPolicy(TimeBasedRollingPolicy) {
        fileNamePattern = "logs/server.%d{yyyy-MM-dd}.log"
    }

    encoder(PatternLayoutEncoder) {
        pattern = "%date{ISO8601} %-5level %logger{36} %X{sourceThread} - %msg%n"
    }

    filter(ThresholdFilter) {
        level = "INFO"
    }
}

logger("com.github.akovari", DEBUG)
logger("akka", WARN)
logger("scala.slick", INFO)
logger("scala.slick.jdbc.JdbcBackend.statement", DEBUG)
logger("org.jooq", DEBUG)
logger("com.zaxxer.hikari.pool", INFO)

root(DEBUG, ["STDOUT", "FILE"])
