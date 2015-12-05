package com.github.akovari.rdfp.data

import com.github.akovari.rdfp.Configuration
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

import scala.concurrent.{ExecutionContext, Future}
import org.jooq._
import org.jooq.impl._

/**
  * User: akovari
  * Date: 13.6.2014
  * Time: 10:39
  */
object DatabaseConnection {
  val cfg = Configuration()

  private val ds = {
    val config = new HikariConfig()
    config.setJdbcUrl(cfg.getString("postgres.url"))
    config.setUsername(cfg.getString("postgres.user"))
    config.setPassword(cfg.getString("postgres.password"))
    config.addDataSourceProperty("cachePrepStmts", "true")
    config.addDataSourceProperty("prepStmtCacheSize", cfg.getString("postgres.maxStatements"))
    config.addDataSourceProperty("prepStmtCacheSqlLimit", cfg.getString("postgres.prepStmtCacheSqlLimit"))
    config.addDataSourceProperty("useServerPrepStmts", "true")
    config.setMaximumPoolSize(cfg.getInt("postgres.maxPoolSize"))
    config.setPoolName("postgres-connection-pool")
    config.setLeakDetectionThreshold(cfg.getLong("postgres.leakDetectionThreshold"))
    config.setConnectionTestQuery(cfg.getString("postgres.connectionTestQuery"))

    new HikariDataSource(config)
  }
  val dslContext = DSL.using(ds, SQLDialect.POSTGRES)

  def transaction[T](f: DSLContext => T)(implicit executionContext: ExecutionContext): Future[T] = dsl.map { nonTxDsl =>
    nonTxDsl.transactionResult(new TransactionalCallable[T] {
      override def run(configuration: Configuration): T = {
        f(DSL.using(configuration))
      }
    })
  }

  def dsl(implicit executionContext: ExecutionContext): Future[DSLContext] = Future.successful(dslContext)
}
