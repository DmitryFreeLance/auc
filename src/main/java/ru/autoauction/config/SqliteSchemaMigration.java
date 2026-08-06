package ru.autoauction.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SqliteSchemaMigration implements ApplicationRunner {
  private static final Logger log=LoggerFactory.getLogger(SqliteSchemaMigration.class);
  private final DataSource dataSource;
  public SqliteSchemaMigration(DataSource dataSource){this.dataSource=dataSource;}

  @Override public void run(ApplicationArguments args) throws Exception {
    try(Connection connection=dataSource.getConnection();Statement statement=connection.createStatement()){
      String ddl=null;
      try(ResultSet result=statement.executeQuery("select sql from sqlite_master where type='table' and name='lots'")){if(result.next())ddl=result.getString(1);}
      if(ddl==null||ddl.contains("'CANCELLED'"))return;
      String upgraded=ddl.replaceFirst("(?i)CREATE\\s+TABLE\\s+[`\\\"]?lots[`\\\"]?","CREATE TABLE lots_status_v2")
          .replaceFirst("(?i)status\\s+in\\s*\\(\\s*'DRAFT'\\s*,\\s*'LIVE'\\s*,\\s*'FINISHED'\\s*\\)","status in ('DRAFT','LIVE','FINISHED','CANCELLED')");
      if(upgraded.equals(ddl)||!upgraded.contains("lots_status_v2")||!upgraded.contains("'CANCELLED'"))throw new IllegalStateException("Не удалось подготовить миграцию статусов таблицы lots");
      statement.execute("PRAGMA foreign_keys=OFF");
      try{
        statement.execute("BEGIN IMMEDIATE");
        statement.execute("DROP TABLE IF EXISTS lots_status_v2");
        statement.execute(upgraded);
        statement.execute("INSERT INTO lots_status_v2 SELECT * FROM lots");
        statement.execute("DROP TABLE lots");
        statement.execute("ALTER TABLE lots_status_v2 RENAME TO lots");
        statement.execute("COMMIT");
        log.info("SQLite: ограничение статусов лотов обновлено, CANCELLED доступен");
      }catch(Exception e){try{statement.execute("ROLLBACK");}catch(Exception ignored){}throw e;}
      finally{statement.execute("PRAGMA foreign_keys=ON");}
    }
  }
}
