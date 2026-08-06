package ru.autoauction.config;

import org.junit.jupiter.api.Test;
import org.sqlite.SQLiteDataSource;
import org.springframework.boot.ApplicationArguments;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class SqliteSchemaMigrationTest {
  @Test void addsCancelledToExistingLotsStatusConstraint() throws Exception {
    SQLiteDataSource dataSource=new SQLiteDataSource();dataSource.setUrl("jdbc:sqlite:file:migration-test?mode=memory&cache=shared");
    try(var keepAlive=dataSource.getConnection();var statement=keepAlive.createStatement()){
      statement.execute("create table lots (id integer primary key, status varchar(20) not null check (status in ('DRAFT','LIVE','FINISHED')))");
      statement.execute("insert into lots(id,status) values (1,'LIVE')");
      new SqliteSchemaMigration(dataSource).run(mock(ApplicationArguments.class));
      assertThatCode(()->statement.execute("update lots set status='CANCELLED' where id=1")).doesNotThrowAnyException();
    }
  }
}
