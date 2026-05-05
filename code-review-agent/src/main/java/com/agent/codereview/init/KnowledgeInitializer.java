package com.agent.codereview.init;

import com.agent.codereview.service.KnowledgeVectorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeInitializer implements CommandLineRunner {

    @Autowired
    private KnowledgeVectorService knowledgeVectorService;

    @Override
    public void run(String... args) {
        System.out.println("开始初始化知识库...");

        try {
            // 导入常用编码规范（每条都会调用 Embedding API，需要网络）
            knowledgeVectorService.addDocument(
                    "空指针规范",
                    "【规范】任何可能为 null 的对象在调用方法前必须进行非空判断。推荐使用 Optional 或 if (obj != null) 检查。"
            );
            knowledgeVectorService.addDocument(
                    "异常处理规范",
                    "【规范】捕获异常后不能空处理（空的 catch 块），必须记录日志或重新抛出更具体的异常。"
            );
            knowledgeVectorService.addDocument(
                    "日志规范",
                    "【规范】生产环境禁止使用 System.out.println 或 e.printStackTrace()，应使用 SLF4J 日志框架，并按级别输出。"
            );
            knowledgeVectorService.addDocument(
                    "SQL 注入防范",
                    "【安全规范】必须使用参数化查询（PreparedStatement）或 MyBatis 的 #{} 方式，禁止拼接用户输入到 SQL 语句。"
            );
            knowledgeVectorService.addDocument(
                    "性能规范",
                    "【规范】循环中禁止频繁调用数据库或远程服务，应将查询提前到循环外部。对于集合操作，注意时间复杂度。"
            );

            System.out.println("知识库初始化完成，已导入示例规范文档。");
        } catch (Exception e) {
            System.err.println("知识库初始化失败，但应用程序将继续运行。错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}