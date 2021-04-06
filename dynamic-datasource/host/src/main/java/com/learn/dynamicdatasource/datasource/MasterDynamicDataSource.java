package com.learn.dynamicdatasource.datasource;

import com.baomidou.dynamic.datasource.provider.AbstractDataSourceProvider;
import com.baomidou.dynamic.datasource.provider.AbstractJdbcDataSourceProvider;
import com.baomidou.dynamic.datasource.provider.DynamicDataSourceProvider;
import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DataSourceProperty;
import com.learn.dynamicdatasource.config.DynamicDataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dz <895180729@qq.com>
 * @Description
 * @Version V1.0.0
 * @Since 1.8
 * @Date 2021/4/6 2:29 下午
 */
@Slf4j
@Component
public class MasterDynamicDataSource extends AbstractDataSourceProvider {

    @Resource
    DynamicDataSourceConfig dynamicDataSourceConfig;

    Map<String, DataSource> dataSourceMap = null;

    @Bean(name = "dynamicDataSourceProvider")
    public DynamicDataSourceProvider dynamicDataSourceProvider() {
        return new MasterDynamicDataSource();
    }

    @Override
    public Map<String, DataSource> loadDataSources() {
        if (dataSourceMap == null) {
            dataSourceMap = new HashMap<>();
            Connection conn = null;
            Statement stmt = null;
            try {
                // 由于 SPI 的支持，现在已无需显示加载驱动了
                // 但在用户显示配置的情况下，进行主动加载
                if (!StringUtils.isEmpty(dynamicDataSourceConfig.getDriverClassName())) {
                    Class.forName(dynamicDataSourceConfig.getDriverClassName());
                    log.info("成功加载数据库驱动程序");
                }
                conn = DriverManager.getConnection(dynamicDataSourceConfig.getUrl(), dynamicDataSourceConfig.getUsername(), dynamicDataSourceConfig.getPassword());
                log.info("成功获取数据库连接");
                stmt = conn.createStatement();
                Map<String, DataSourceProperty> dataSourcePropertiesMap = executeStmt(stmt);
                dataSourceMap = createDataSourceMap(dataSourcePropertiesMap);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                JdbcUtils.closeConnection(conn);
                JdbcUtils.closeStatement(stmt);
            }
        }
        return dataSourceMap;
    }

    private Map<String, DataSourceProperty> executeStmt(Statement statement) throws SQLException {
        log.info("111");
        ResultSet rs = statement.executeQuery(String.format("select * from %s", dynamicDataSourceConfig.getTableName()));
        Map<String, DataSourceProperty> map = new HashMap<>();

        while (rs.next()) {
            String name = rs.getString("name");
            String username = rs.getString("username");
            String password = rs.getString("password");
            String url = rs.getString("url");
            String driver = rs.getString("driver");
            DataSourceProperty property = new DataSourceProperty();
            property.setUsername(username);
            property.setPassword(password);
            property.setUrl(url);
            property.setDriverClassName(driver);
            map.put("my_" + name, property);
        }
        // 添加当前数据源
        DataSourceProperty property = new DataSourceProperty();
        property.setUsername(dynamicDataSourceConfig.getUsername());
        property.setPassword(dynamicDataSourceConfig.getPassword());
        property.setUrl(dynamicDataSourceConfig.getUrl());
        property.setDriverClassName(dynamicDataSourceConfig.getDriverClassName());
        map.put("master", property);

        return map;
    }
}
