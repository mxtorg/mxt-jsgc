基于 JSON Schema 自动生成 API 制品的 Maven 插件 – 开发说明文档
第一部分：Step by Step 需求梳理
1. 背景与目标
开发一个 Maven 插件，输入一个自定义的 JSON Schema 文件（扩展自 OpenAPI，包含 git、gav 等元信息），自动生成以下制品：

Spring Boot Controller 桩代码（含 Swagger/OpenAPI 注解）

OpenFeign 客户端接口

DTO 类（含 JSR-303 校验注解）

API 文档（如 Markdown 或 HTML）

Mock 服务器配置（如 WireMock 或 Spring Cloud Contract）

生成的代码需符合命名、注解、方法签名等规则，并能自动推送至指定 Git 仓库及发布到 Maven 仓库。

2. 输入 JSON Schema 结构解析
根对象必须包含：$schema, info, paths, components，以及扩展字段 git, gav。

git：代码仓库信息（url, branch, token），用于推送生成的源代码。

gav：Maven 坐标（gav）和发布仓库列表（repository），用于发布 API 依赖包。

paths：定义 REST 端点，每个路径下包含一个或多个 HTTP 方法（post, put, get, delete 等），每个方法对象包含 summary, request, responses 等。

components/schemas：定义可复用的 DTO 和 Controller 类（如 DemoEndpoint 作为 Controller 类，没有 properties）。DTO 类支持 JSON Schema 校验关键字（minLength, format 等）。

3. 核心需求（逐项）
3.1 解析与验证

读取 JSON 文件，验证必填字段和结构正确性。

支持 $ref 引用解析（内部引用 #/components/schemas/...）。

3.2 生成 Controller 类

每个在 paths 中被 schema.$ref 引用的 components/schemas 类（如 DemoEndpoint）生成一个独立的 Controller 类。

类名：引用 schema 的名称（如 DemoEndpoint）。

类注解：@RestController, @Tag(name = "类的描述"), @Slf4j（可选）。

方法生成：

方法名：httpMethod + 路径（驼峰式，例如 postUserInstance、putUserInstance、getUserInstance）。路径中的斜杠和特殊字符需转换（保留字母数字，首字母小写后驼峰）。

路由注解：根据 HTTP 方法生成 @PostMapping, @PutMapping, @GetMapping, @DeleteMapping，值为原始路径（如 "/users/instance"）。

若方法对象中包含 summary，生成 @Operation(summary = "summary内容")。

入参处理：

POST/PUT：如果 request.content 存在且 schema 为对象，生成 @RequestBody 参数，类型为引用的 DTO 类。

GET/DELETE：遍历 parameters 数组，每个生成 @RequestParam 参数（name, required, schema 类型）。需要处理基本类型和 format。

出参处理：根据 responses 中某个状态码的 content 的 schema 确定返回类型。若为引用 DTO，返回该类型；若为基本类型（如 type: integer），返回 Java 包装类型（Long/Integer）。

方法体生成桩代码：// TODO 注释 + 简单的日志记录 + 默认返回值（如 null 或 0）。

3.3 生成 DTO 类

遍历 components/schemas 中所有非 Controller 的 schema 对象（即被用作 request/response 但本身没有 path 映射的类）。

类名：schema 名称。

字段：根据 properties 生成，类型映射（string→String, integer→Long/Integer, number→BigDecimal, boolean→Boolean, array→List<...>）。

校验注解：根据 JSON Schema 关键字生成 JSR-303 注解：

minLength → @Size(min = value)

format: email → @Email

required → @NotNull（或 @NotBlank 对于 string）

支持 pattern → @Pattern, minimum/maximum → @Min/@Max 等。

生成 getter/setter（Lombok 或手动）。

生成 toString 等通用方法。

3.4 生成 Feign 客户端接口

名称规则：<Controller类名>Client（例如 DemoClient）。

接口注解：@FeignClient(name = "...", url = "${api.<name>.url:}")。name 从 info.title 或类名派生。

每个 Controller 方法在 Client 中生成对应方法（方法签名、注解、路径完全一致，但参数为 @RequestBody/@RequestParam，且没有方法体）。

注意：Feign 接口中 POST/PUT 的 @RequestBody 参数可以显式命名，无需 @Valid（按需）。

3.5 生成 API 文档

从 info, paths, components 提取内容，生成 Markdown 或 AsciiDoc 格式。

文档包含：接口标题、版本、每个端点的 URL、方法、请求参数/体、响应示例。

可集成 Swagger/OpenAPI 静态文档生成器，或直接用模板输出。

3.6 生成 Mock 服务器配置

根据 paths 生成 WireMock stubbing JSON 或 Spring Cloud Contract Groovy 文件。

每个 HTTP 方法对应一个 mock 响应（根据 responses 中的示例状态码和内容 schema 生成示例数据）。

Mock 配置可抽离为独立文件，供集成测试使用。

3.7 集成 Maven 构建生命周期

定义插件 Mojo，绑定到 generate-sources 阶段。

配置参数：schemaFile（JSON 文件路径）, outputDirectory（生成的 Java 源码目录）, apiDocDir（文档目录）, mockDir 等。

自动在 target/generated-sources 生成 Java 代码，并添加为项目源根。

3.8 Git 推送与 Maven 发布

生成代码后，将代码（Controller、Client、DTO）提交到 git 指定的仓库（分支、token 认证）。

使用 Maven 发布插件（maven-deploy-plugin）将生成的 API 包（仅包含 DTO 和 Feign 接口，不含 Controller？根据需求可能是整个 API 模块）发布到 gav.repository 中的 Maven 仓库（releases/snapshots 根据版本判断）。

注意：生成的代码应独立为一个 Maven 子模块，便于发布。

第二部分：Spec 编程规则文档（规格说明）
1. 插件元数据
groupId: org.demo.cloud

artifactId: api-generator-maven-plugin

version: 1.0.0

goal前缀: api

2. Mojo 配置参数
参数名	类型	必填	默认值	描述
schemaFile	File	是	${basedir}/src/main/api/api-schema.json	输入的 JSON Schema 文件路径
outputJavaDir	File	否	${project.build.directory}/generated-sources/api	Java 源代码输出根目录
apiDocDir	File	否	${project.build.directory}/generated-docs/api	API 文档输出目录
mockDir	File	否	${project.build.directory}/generated-mocks	Mock 配置输出目录
pushToGit	boolean	否	true	是否将生成的代码推送到 Git 仓库
deployMaven	boolean	否	true	是否将生成的 API 包发布到 Maven 仓库
3. 代码生成规则（详细）
3.1 Java 类型映射（JSON Schema → Java）
JSON Schema Type	Format	Java Type	校验注解（示例）
string	-	String	@Size, @NotBlank
string	email	String	@Email
string	date	LocalDate	@Past / @Future
string	date-time	ZonedDateTime	-
integer	int32	Integer	@Min, @Max
integer	int64	Long	@Min, @Max
number	float	Float	@DecimalMin
number	double	Double	@DecimalMin
boolean	-	Boolean	-
array	-	List<T>	@Size for list
object	-	对应 DTO 类	-
3.2 Controller 方法命名规则
路径中的 / 和 - 被移除，后续单词首字母大写，但整体首字母小写（驼峰）。

例如：路径 /users/instance → 方法基名 userInstance，加上 HTTP 方法前缀：postUserInstance, putUserInstance, getUserInstance。

若路径包含变量（如 /users/{id}），变量部分忽略，保持相同规则。

3.3 参数注解规则
HTTP 方法	参数位置	生成的注解	示例
POST/PUT	request body	@RequestBody	public Long postUserInstance(@RequestBody RegisterRequest request)
GET/DELETE	query parameter	@RequestParam	public UserResponse getUserInstance(@RequestParam("id") Long id)
任意	path variable	@PathVariable	（若路径中有 {id}，则从路径提取）
3.4 响应类型推断规则
遍历 responses 中的 HTTP 状态码，优先取 2xx 或 default。

若 content.application/json.schema 存在：

若 $ref → 对应 DTO 类型。

若为内联 schema 且 type: integer → Long，string → String，object 则生成匿名内联类（不推荐，自动转为 Object）。

若没有 content，返回类型为 Void。

3.5 DTO 校验注解详细映射
minLength / maxLength → @Size(min=..., max=...)

pattern → @Pattern(regexp=...)

minimum / maximum → @Min / @Max（exclusiveMinimum 需使用 @DecimalMin(value, inclusive=false)）

required 数组中的字段 → @NotNull 或 @NotBlank（若类型为 string）

format: email → @Email

数组的 minItems / maxItems → @Size 作用于 List 字段

3.6 OpenFeign 客户端生成规则
接口名：${Controller类名}Client，所有客户端集中在一个包（如 org.demo.cloud.client）。

类注解：@FeignClient(name = "${apiName}", url = "${api.${apiName}.url:}")，其中 apiName 为 info.title 去除空格后的驼峰。

方法签名、路由注解、参数注解与 Controller 方法完全一致（仅移除方法体）。

不生成 @Valid 注解（由调用方决定）。

4. Git 交互规则
使用 JGit 库执行 clone/push。

仓库 URL 从 git.url 读取，分支从 git.branch 读取，认证 token 从 git.token 读取（支持 http 头 Authorization: token xxx）。

推送路径：生成的代码默认推送到仓库根目录下的 api-module 文件夹，可配置。

每次生成时，先拉取最新代码，覆盖生成的内容（建议先删除目标文件夹），然后 commit + push。Commit message 建议为 chore: regenerate API stubs from schema。

5. Maven 发布规则
生成的 API 模块应包含 pom.xml，其 GAV 从 gav.gav 解析（格式 groupId:artifactId:version）。

发布仓库从 gav.repository 获取，区分 releases/snapshots（version 含 SNAPSHOT 则发往 snapshot 仓库，否则 releases）。

使用 Maven Wagon 或 aether 进行 deploy，或直接调用 mvn deploy 命令（需确保已配置仓库认证）。

6. 插件执行流程（伪代码）
pseudo
read schema file
validate schema
parse paths and components

for each controllerSchema in components.schemas where referenced by any path:
  generate Controller class
  generate Feign client class

for each dtoSchema in components.schemas not used as controller:
  generate DTO class with validations

generate api doc (markdown)
generate mock config (wiremock json)

if pushToGit:
  clone or update git repo, copy generated files, commit, push

if deployMaven:
  create temporary pom with gav and repositories, deploy jar (generated classes), clean
7. 测试验证点
输入示例 JSON，生成的 Controller 方法签名、注解、DTO 字段和校验注解与预期一致。

Feign 客户端接口与 Controller 方法一一对应，没有多余或缺失。

生成的代码编译通过（无语法错误）。

Git 推送成功且不覆盖无关文件。

Maven 发布成功，其他项目引入该 API 包后能正常使用 Feign 客户端。

8. 扩展性与约束
仅支持 JSON Schema Draft-07 子集（类型、校验关键字、$ref）。

不支持 allOf/anyOf/oneOf/not 等复合结构（初期可报错或忽略）。

不生成 Controller 业务逻辑，仅生成桩代码及 TODO 注释。

生成的代码中，JSR-303 注解需要引入 javax.validation 或 jakarta.validation，插件应自动在生成的 pom.xml 中添加对应依赖。

9. 示例用法（在目标项目中的配置）
xml
<plugin>
  <groupId>org.demo.cloud</groupId>
  <artifactId>api-generator-maven-plugin</artifactId>
  <version>1.0.0</version>
  <executions>
    <execution>
      <goals>
        <goal>generate</goal>
      </goals>
      <configuration>
        <schemaFile>${project.basedir}/api-definition.json</schemaFile>
      </configuration>
    </execution>
  </executions>
</plugin>