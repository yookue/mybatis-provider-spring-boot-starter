# Mybatis Provider Spring Boot Starter

Spring Boot application integrates `mybatis` quickly, to support different statements of different databases.

## Quickstart

- Import dependencies

```xml
    <dependency>
        <groupId>com.yookue.springstarter</groupId>
        <artifactId>mybatis-provider-spring-boot-starter</artifactId>
        <version>LATEST</version>
    </dependency>
```

> By default, this starter will auto take effect, you can turn it off by `spring.mybatis-provider.enabled = false`

- Configure Spring Boot `application.yml` with prefix `spring.mybatis-provider` (**Optional**)

```yml
spring:
    mybatis-provider:
        configFile: 'classpath:/META-INF/mybatis/database-id-provider.xml'
```

This will create a `DatabaseIdProvider` bean, which supports most popular relational databases in the world.

- Write your mybatis mapper statements as following:

```xml
<select id="foo" resultType="bar" databaseId="mysql">
</select>
```

> Note that the `databaseId` segment, that is the database identifier from your `configFile` node of the previous step.

## Document

- Github: https://github.com/yookue/mybatis-provider-spring-boot-starter
- Mybatis homepage: https://mybatis.org/mybatis-3
- Mybatis github: https://github.com/mybatis/mybatis-3
- Relational databases: https://db-engines.com/en/ranking/relational+dbms

## Requirement

- jdk 1.8+

## License

This project is under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

See the `NOTICE.txt` file for required notices and attributions.

## Donation

You like this package? Then [donate to Yookue](https://yookue.com/public/donate) to support the development.

## Website

- Yookue: https://yookue.com

