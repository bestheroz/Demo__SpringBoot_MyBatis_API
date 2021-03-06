import com.github.bestheroz.standard.common.util.CaseUtils;
import com.github.bestheroz.standard.context.db.checker.DbTableVOCheckerContext;
import com.github.bestheroz.standard.context.web.WebConfiguration;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.text.MessageFormat;
import javax.annotation.Resource;
import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.AutoConfigureMybatis;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest(classes = {WebConfiguration.class})
@AutoConfigureMybatis
public class TestCreateTableEntity {
  private final String tableName = "MEMBER_MENU";
  private final String javaPackageEndPoint = this.tableName.replaceAll("_", ".").toLowerCase();
  private final String javaProjectRootPackageName = "com.github.bestheroz.";
  private final String javaPackageName = this.javaProjectRootPackageName + "demo.entity.";
  private final String javaFilePath =
      "src/main/java/" + this.javaPackageName.replaceAll("\\.", "/");
  private final String tsFilePath = "vue/src/common/types.ts";

  @Qualifier("dataSource")
  @Resource
  private DataSource dataSource;

  @Test
  public void makeEntityRepositoryFilesAuto() {
    try (final Statement stmt = this.dataSource.getConnection().createStatement()) {
      try (final ResultSet rs = stmt.executeQuery("SELECT * FROM " + this.tableName + " LIMIT 0")) {
        final String tableEntityName = CaseUtils.getSnakeCaseToCamelCase(this.tableName);

        final ResultSetMetaData metaInfo = rs.getMetaData();
        System.out.println(tableEntityName);
        // 1. VO?????????
        final StringBuilder javaString = new StringBuilder();
        final StringBuilder tsString = new StringBuilder();
        for (int i = 0; i < metaInfo.getColumnCount(); i++) {
          final String fieldType;
          final String tsType;
          final String columnTypeName = metaInfo.getColumnTypeName(i + 1);
          final String columnName = metaInfo.getColumnName(i + 1);
          final String camelColumnName = CaseUtils.getSnakeCaseToCamelCase(columnName);
          if (DbTableVOCheckerContext.STRING_JDBC_TYPE_SET.contains(columnTypeName)) {
            fieldType = "String";
            tsType = "string";
          } else if (StringUtils.equalsAny(
              columnTypeName,
              "NUMBER",
              "DECIMAL",
              "BIGINT UNSIGNED",
              "BIGINT",
              "SMALLINT UNSIGNED",
              "SMALLINT")) {
            if (metaInfo.getScale(i + 1) > 0) { // ???????????? ?????????
              fieldType = "Double";
            } else {
              final int columnDisplaySize =
                  metaInfo.getColumnDisplaySize(i + 1); // ?????? ??????????????? ????????? ??? ??????(????????? "." ?????????????)
              if (columnDisplaySize <= 5) { // 5?????? ????????????, 4?????? ??????
                fieldType = "Short"; // ???32,768 ~ 32,767
              } else if (columnDisplaySize <= 10) { // 10?????? ????????????, 9?????? ??????
                fieldType = "Integer"; // ???2,147,483,648 ~ 2,147,483,647
                // } else if (columnDisplaySize < 19) {
                // 19?????? ????????????, 18?????? ??????, ????????? (?????????) NUMBER??? ???????????? 39???.. Long ?????? ????????????.
                // fieldType = "Long"; // -9223372036854775808 ~ 9223372036854775807
              } else { // 19?????? ????????????, 18?????? ??????, ????????? (?????????) NUMBER??? ???????????? 39???.. Long ?????? ????????????.
                fieldType = "Long";
                // fieldType = "Double";
              }
            }
            tsType = "number";
          } else if (DbTableVOCheckerContext.NUMBER_JDBC_TYPE_SET.contains(columnTypeName)) {
            fieldType = "Integer";
            tsType = "number";
          } else if (DbTableVOCheckerContext.DATETIME_JDBC_TYPE_SET.contains(columnTypeName)) {
            fieldType = DbTableVOCheckerContext.DEFAULT_DATE_TYPE;
            tsType = "Date | string";
          } else if (DbTableVOCheckerContext.BOOLEAN_JDBC_TYPE_SET.contains(columnTypeName)) {
            fieldType = "Boolean";
            tsType = "boolean";
          } else if (DbTableVOCheckerContext.BYTE_JDBC_TYPE_SET.contains(columnTypeName)) {
            fieldType = "Byte[];";
            tsType = "any[]";
            log.debug(
                "private Byte[] {}{}",
                camelColumnName,
                "; // XXX: spotbugs ????????? : Arrays.copyOf(value, value.length)");
          } else {
            fieldType = "Unknown";
            tsType = "unknown";
            log.warn("????????? ?????? {} : {}", columnName, columnTypeName);
          }
          javaString
              .append("private ")
              .append(fieldType)
              .append(" ")
              .append(camelColumnName)
              .append(";\n");
          tsString.append(camelColumnName).append("?: ").append(tsType).append(" | null;\n");
        }
        this.writeToFile(tableEntityName, javaString, tsString);
      }
    } catch (final Throwable e) {
      log.warn(ExceptionUtils.getStackTrace(e));
    }
  }

  private void writeToFile(
      final String entityName, final StringBuilder javaString, final StringBuilder tsString)
      throws IOException {
    final boolean hasAbstractCreatedUpdate =
        StringUtils.contains(javaString, " createdBy")
            && StringUtils.contains(javaString, " created")
            && StringUtils.contains(javaString, " updatedBy")
            && StringUtils.contains(javaString, " updated");
    final String javaHeader =
        MessageFormat.format(
            """
        package {0}{1};

        {2}
        import lombok.AllArgsConstructor;
        import lombok.AllArgsConstructor;
        import lombok.Data;
        import lombok.EqualsAndHashCode;
        import lombok.NoArgsConstructor;
        import java.io.Serializable;

        {3}
        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public class {4}{5} implements Serializable {
        """,
            this.javaPackageName,
            this.javaPackageEndPoint,
            hasAbstractCreatedUpdate
                ? "import " + this.javaPackageName + "AbstractCreatedUpdate;"
                : "",
            hasAbstractCreatedUpdate ? "@EqualsAndHashCode(callSuper = true)" : "",
            entityName,
            hasAbstractCreatedUpdate ? " extends AbstractCreatedUpdate" : "");
    final String javaBody =
        hasAbstractCreatedUpdate
            ? javaString
                .toString()
                .replace("private String createdBy;\n", "")
                .replace("private Instant created;\n", "")
                .replace("private String updatedBy;\n", "")
                .replace("private Instant updated;\n", "")
            : javaString.toString();
    if (Files.notExists(
        Paths.get(this.javaFilePath + this.javaPackageEndPoint.replaceAll("\\.", "/")))) {
      FileUtils.forceMkdir(
          Paths.get(this.javaFilePath + this.javaPackageEndPoint.replaceAll("\\.", "/")).toFile());
    }
    final Path javaEntityFilePath =
        Paths.get(
            this.javaFilePath
                + this.javaPackageEndPoint.replaceAll("\\.", "/")
                + "/"
                + entityName
                + ".java");
    Files.write(
        javaEntityFilePath,
        (javaHeader + javaBody + "}").getBytes(),
        Files.notExists(javaEntityFilePath)
            ? StandardOpenOption.CREATE_NEW
            : StandardOpenOption.TRUNCATE_EXISTING);
    final String repositoryHeader =
        MessageFormat.format(
            """
        package {0}{1};

        import {2}standard.common.mybatis.SqlRepository;
        import org.apache.ibatis.annotations.Mapper;
        import org.springframework.stereotype.Repository;

        @Mapper
        @Repository

        public interface {3} extends SqlRepository<{4}> {
        }
        """,
            this.javaPackageName,
            this.javaPackageEndPoint,
            this.javaProjectRootPackageName,
            entityName + "Repository",
            entityName);
    final Path javaRepositoryFilePath =
        Paths.get(
            this.javaFilePath
                + this.javaPackageEndPoint.replaceAll("\\.", "/")
                + "/"
                + entityName
                + "Repository"
                + ".java");
    Files.write(
        javaRepositoryFilePath,
        repositoryHeader.getBytes(),
        Files.notExists(javaRepositoryFilePath)
            ? StandardOpenOption.CREATE_NEW
            : StandardOpenOption.TRUNCATE_EXISTING);
    Files.write(
        Paths.get(this.tsFilePath),
        MessageFormat.format(
                """
                   export interface {0} {
                   {1}}
                   """,
                entityName, tsString)
            .getBytes(),
        StandardOpenOption.APPEND);
  }
}
