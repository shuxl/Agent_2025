# MCP Server 安全改造方案

## 项目现状分析

### 当前安全配置
- **认证方式**: OAuth2 + JWT
- **授权模式**: 客户端凭据模式 (Client Credentials)
- **权限控制**: 全局认证 (`anyRequest().authenticated()`)
- **CSRF**: 已禁用（适合API服务）
- **客户端配置**: 
  - Client ID: `mcp-client`
  - Client Secret: `secret` (开发环境未加密)

### 当前端点
- `/oauth2/token` - OAuth2令牌获取
- `/mcp/message` - MCP消息端点
- `/api/mcp/status` - 服务状态
- `/api/mcp/health` - 健康检查

## 1. 细粒度权限控制改造方案

### 1.1 URL级别权限控制

#### 改造SecurityConfiguration
```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // 启用方法级安全
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .authorizeHttpRequests(auth -> auth
                // 公开端点
                .requestMatchers("/api/mcp/health").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                
                // 管理端点 - 需要管理员权限
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // MCP端点 - 需要MCP权限
                .requestMatchers("/mcp/**").hasAuthority("MCP_ACCESS")
                
                // API端点 - 需要API权限
                .requestMatchers("/api/**").hasAuthority("API_ACCESS")
                
                // 其他请求需要认证
                .anyRequest().authenticated()
            )
            .with(authorizationServer(), Customizer.withDefaults())
            .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()))
            .csrf(CsrfConfigurer::disable)
            .cors(Customizer.withDefaults())
            .build();
    }
}
```

### 1.2 方法级权限控制

#### 改造McpController
```java
@RestController
@RequestMapping("/api/mcp")
@PreAuthorize("hasAuthority('API_ACCESS')")
public class McpController {

    @GetMapping("/status")
    @PreAuthorize("hasAuthority('READ_STATUS')")
    public Map<String, Object> getStatus() {
        // 实现逻辑
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        // 健康检查保持公开
    }
    
    @PostMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> updateConfig(@RequestBody ConfigDto config) {
        // 管理员专用配置更新
    }
}
```

### 1.3 数据级权限控制

#### 创建权限服务
```java
@Service
public class McpPermissionService {
    
    @Autowired
    private ClientRepository clientRepository;
    
    public boolean hasAccessToTool(String clientId, String toolName) {
        Client client = clientRepository.findByClientId(clientId);
        return client.getAuthorizedTools().contains(toolName);
    }
    
    public boolean hasAccessToResource(String clientId, String resourceId) {
        Client client = clientRepository.findByClientId(clientId);
        return client.getAuthorizedResources().contains(resourceId);
    }
}
```

#### 改造WeatherService
```java
@Service
@McpTool("weather-service")
public class WeatherService {
    
    @Autowired
    private McpPermissionService permissionService;
    
    @Tool("getWeatherForecast")
    @PreAuthorize("@mcpPermissionService.hasAccessToTool(authentication.name, 'weather-forecast')")
    public Forecast getWeatherForecast(@P("location") String location) {
        // 实现逻辑
    }
    
    @Tool("getWeatherAlerts")
    @PreAuthorize("@mcpPermissionService.hasAccessToTool(authentication.name, 'weather-alerts')")
    public List<Alert> getWeatherAlerts(@P("location") String location) {
        // 实现逻辑
    }
}
```

### 1.4 JWT Claims扩展

#### 创建JWT Claims处理器
```java
@Component
public class McpJwtClaimsProcessor {
    
    public Set<String> extractAuthorities(Jwt jwt) {
        return jwt.getClaimAsStringList("authorities").stream()
            .collect(Collectors.toSet());
    }
    
    public Set<String> extractTools(Jwt jwt) {
        return jwt.getClaimAsStringList("authorized_tools").stream()
            .collect(Collectors.toSet());
    }
    
    public Set<String> extractResources(Jwt jwt) {
        return jwt.getClaimAsStringList("authorized_resources").stream()
            .collect(Collectors.toSet());
    }
    
    public String extractClientId(Jwt jwt) {
        return jwt.getClaimAsString("client_id");
    }
}
```

## 2. OAuth2生产环境改造方案

### 2.1 客户端凭据模式升级

#### 当前模式评估
✅ **客户端凭据模式适合生产环境**，因为：
- MCP Server主要用于服务间通信
- 不需要用户交互
- 安全性高，密钥管理相对简单

#### 下一步改造方案

### 2.2 客户端管理增强

#### 创建客户端实体
```java
@Entity
@Table(name = "oauth_clients")
public class OAuthClient {
    @Id
    private String clientId;
    
    @Column(nullable = false)
    private String clientSecret;
    
    @ElementCollection
    @CollectionTable(name = "client_authorities")
    private Set<String> authorities = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "client_tools")
    private Set<String> authorizedTools = new HashSet<>();
    
    @ElementCollection
    @CollectionTable(name = "client_resources")
    private Set<String> authorizedResources = new HashSet<>();
    
    @Column
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime expiresAt;
    
    @Column
    private boolean active = true;
    
    // getters and setters
}
```

#### 创建客户端管理服务
```java
@Service
public class ClientManagementService {
    
    @Autowired
    private OAuthClientRepository clientRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    public OAuthClient createClient(ClientRegistrationDto dto) {
        OAuthClient client = new OAuthClient();
        client.setClientId(dto.getClientId());
        client.setClientSecret(passwordEncoder.encode(dto.getClientSecret()));
        client.setAuthorities(dto.getAuthorities());
        client.setAuthorizedTools(dto.getAuthorizedTools());
        client.setAuthorizedResources(dto.getAuthorizedResources());
        client.setCreatedAt(LocalDateTime.now());
        client.setExpiresAt(dto.getExpiresAt());
        
        return clientRepository.save(client);
    }
    
    public void revokeClient(String clientId) {
        OAuthClient client = clientRepository.findByClientId(clientId);
        client.setActive(false);
        clientRepository.save(client);
    }
    
    public void updateClientPermissions(String clientId, Set<String> tools, Set<String> resources) {
        OAuthClient client = clientRepository.findByClientId(clientId);
        client.setAuthorizedTools(tools);
        client.setAuthorizedResources(resources);
        clientRepository.save(client);
    }
}
```

### 2.3 动态客户端配置

#### 创建自定义客户端配置
```java
@Configuration
public class DynamicClientConfiguration {
    
    @Autowired
    private ClientManagementService clientService;
    
    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        return new InMemoryRegisteredClientRepository() {
            @Override
            public RegisteredClient findById(String id) {
                OAuthClient client = clientService.findById(id);
                if (client == null || !client.isActive()) {
                    return null;
                }
                
                return RegisteredClient.withId(client.getClientId())
                    .clientId(client.getClientId())
                    .clientSecret(client.getClientSecret())
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .scope("mcp:read")
                    .scope("mcp:write")
                    .build();
            }
        };
    }
}
```

### 2.4 生产环境配置

#### 更新application.properties
```properties
# 生产环境OAuth2配置
spring.security.oauth2.authorizationserver.client.production-client.registration.client-id=${OAUTH_CLIENT_ID}
spring.security.oauth2.authorizationserver.client.production-client.registration.client-secret={bcrypt}${OAUTH_CLIENT_SECRET_BCRYPT}
spring.security.oauth2.authorizationserver.client.production-client.registration.client-authentication-methods=client_secret_basic
spring.security.oauth2.authorizationserver.client.production-client.registration.authorization-grant-types=client_credentials
spring.security.oauth2.authorizationserver.client.production-client.registration.scope=mcp:read,mcp:write,api:access

# JWT配置
spring.security.oauth2.authorizationserver.jwt.issuer=${JWT_ISSUER:https://your-domain.com}
spring.security.oauth2.authorizationserver.jwt.access-token-time-to-live=PT1H
spring.security.oauth2.authorizationserver.jwt.refresh-token-time-to-live=PT24H

# 数据库配置
spring.datasource.url=${DATABASE_URL}
spring.datasource.username=${DATABASE_USERNAME}
spring.datasource.password=${DATABASE_PASSWORD}
spring.jpa.hibernate.ddl-auto=validate
```

### 2.5 密钥管理

#### 环境变量配置
```bash
# 生产环境环境变量
export OAUTH_CLIENT_ID="mcp-production-client"
export OAUTH_CLIENT_SECRET_BCRYPT="$(bcrypt-cli hash 'your-secure-secret')"
export JWT_ISSUER="https://your-mcp-server.com"
export DATABASE_URL="jdbc:postgresql://your-db:5432/mcp_server"
export DATABASE_USERNAME="mcp_user"
export DATABASE_PASSWORD="secure_password"
```

#### 密钥轮换机制
```java
@Service
public class SecretRotationService {
    
    @Scheduled(cron = "0 0 2 1 * ?") // 每月1日凌晨2点
    public void rotateSecrets() {
        List<OAuthClient> clients = clientRepository.findAll();
        
        for (OAuthClient client : clients) {
            String newSecret = generateSecureSecret();
            client.setClientSecret(passwordEncoder.encode(newSecret));
            clientRepository.save(client);
            
            // 通知客户端更新密钥
            notifyClientSecretUpdate(client.getClientId(), newSecret);
        }
    }
    
    private String generateSecureSecret() {
        return UUID.randomUUID().toString() + "-" + 
               System.currentTimeMillis() + "-" + 
               RandomStringUtils.randomAlphanumeric(32);
    }
}
```

### 2.6 监控和审计

#### 创建审计服务
```java
@Service
public class SecurityAuditService {
    
    @Autowired
    private AuditLogRepository auditLogRepository;
    
    public void logTokenRequest(String clientId, boolean success, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setClientId(clientId);
        log.setEventType("TOKEN_REQUEST");
        log.setSuccess(success);
        log.setIpAddress(ipAddress);
        log.setTimestamp(LocalDateTime.now());
        
        auditLogRepository.save(log);
    }
    
    public void logApiAccess(String clientId, String endpoint, boolean success) {
        AuditLog log = new AuditLog();
        log.setClientId(clientId);
        log.setEventType("API_ACCESS");
        log.setEndpoint(endpoint);
        log.setSuccess(success);
        log.setTimestamp(LocalDateTime.now());
        
        auditLogRepository.save(log);
    }
}
```

#### 创建监控端点
```java
@RestController
@RequestMapping("/api/admin/monitoring")
@PreAuthorize("hasRole('ADMIN')")
public class MonitoringController {
    
    @Autowired
    private SecurityAuditService auditService;
    
    @GetMapping("/audit-logs")
    public Page<AuditLog> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditService.getAuditLogs(page, size);
    }
    
    @GetMapping("/client-stats")
    public Map<String, Object> getClientStats() {
        return auditService.getClientStatistics();
    }
}
```

## 3. 实施计划

### 阶段1: 基础权限控制 (1-2周)
1. 改造SecurityConfiguration，实现URL级别权限控制
2. 添加方法级权限注解
3. 创建基础权限服务

### 阶段2: 客户端管理 (2-3周)
1. 实现客户端实体和管理服务
2. 创建客户端注册API
3. 实现动态客户端配置

### 阶段3: 生产环境准备 (1-2周)
1. 配置生产环境参数
2. 实现密钥管理机制
3. 添加监控和审计功能

### 阶段4: 测试和部署 (1周)
1. 全面测试权限控制
2. 性能测试
3. 生产环境部署

## 4. 安全最佳实践

### 4.1 密钥管理
- 使用强密码和定期轮换
- 生产环境使用加密存储
- 实现密钥版本管理

### 4.2 权限最小化
- 按需分配权限
- 定期审查权限
- 实现权限回收机制

### 4.3 监控和告警
- 实时监控异常访问
- 设置安全事件告警
- 定期安全审计

### 4.4 文档和培训
- 提供客户端集成文档
- 安全使用指南
- 定期安全培训

## 5. 风险评估

### 高风险
- 客户端密钥泄露
- 权限配置错误
- JWT令牌泄露

### 中风险
- 权限过度分配
- 监控盲区
- 密钥轮换失败

### 低风险
- 性能影响
- 用户体验
- 维护复杂度

## 6. 总结

通过以上改造方案，MCP Server将具备：

1. **细粒度权限控制**: URL、方法、数据三级权限控制
2. **生产级安全**: 客户端管理、密钥轮换、监控审计
3. **可扩展性**: 支持多客户端、动态配置
4. **可维护性**: 完整的监控和审计体系

客户端凭据模式完全适合生产环境使用，通过增强的权限控制和客户端管理，可以为第三方提供安全可靠的MCP服务。
