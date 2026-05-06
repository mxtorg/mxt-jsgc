package org.demo.maven.model;

import java.util.Map;

/**
 * API配置主模型
 */
public class ApiConfig {
    private Info info;
    private GitConfig git;
    private GavConfig gav;
    private Map<String, PathConfig> paths;
    private Components components;

    public Info getInfo() {
        return info;
    }

    public void setInfo(Info info) {
        this.info = info;
    }

    public GitConfig getGit() {
        return git;
    }

    public void setGit(GitConfig git) {
        this.git = git;
    }

    public GavConfig getGav() {
        return gav;
    }

    public void setGav(GavConfig gav) {
        this.gav = gav;
    }

    public Map<String, PathConfig> getPaths() {
        return paths;
    }

    public void setPaths(Map<String, PathConfig> paths) {
        this.paths = paths;
    }

    public Components getComponents() {
        return components;
    }

    public void setComponents(Components components) {
        this.components = components;
    }

    /**
     * 服务信息
     */
    public static class Info {
        private String title;
        private String description;
        private String version;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }

    /**
     * Git配置
     */
    public static class GitConfig {
        private String url;
        private String branch;
        private String token;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getBranch() {
            return branch;
        }

        public void setBranch(String branch) {
            this.branch = branch;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    /**
     * Maven GAV配置
     */
    public static class GavConfig {
        private String gav;
        private String pkg;
        private Repository[] repository;

        public String getGav() {
            return gav;
        }

        public void setGav(String gav) {
            this.gav = gav;
        }

        public String getPkg() {
            return pkg;
        }

        public void setPkg(String pkg) {
            this.pkg = pkg;
        }

        public Repository[] getRepository() {
            return repository;
        }

        public void setRepository(Repository[] repository) {
            this.repository = repository;
        }

        public static class Repository {
            private String url;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
            }
        }
    }

    /**
     * 路径配置
     */
    public static class PathConfig {
        private SchemaRef schema;
        private OperationConfig get;
        private OperationConfig put;
        private OperationConfig post;
        private OperationConfig delete;

        public SchemaRef getSchema() {
            return schema;
        }

        public void setSchema(SchemaRef schema) {
            this.schema = schema;
        }

        public OperationConfig getGet() {
            return get;
        }

        public void setGet(OperationConfig get) {
            this.get = get;
        }

        public OperationConfig getPut() {
            return put;
        }

        public void setPut(OperationConfig put) {
            this.put = put;
        }

        public OperationConfig getPost() {
            return post;
        }

        public void setPost(OperationConfig post) {
            this.post = post;
        }

        public OperationConfig getDelete() {
            return delete;
        }

        public void setDelete(OperationConfig delete) {
            this.delete = delete;
        }

        public static class SchemaRef {
            private String $ref;

            public String get$ref() {
                return $ref;
            }

            public void set$ref(String $ref) {
                this.$ref = $ref;
            }
        }
    }

    /**
     * 操作配置
     */
    public static class OperationConfig {
        private String summary;
        private ParameterConfig[] parameters;
        private RequestConfig request;
        private Map<String, ResponseConfig> responses;

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public ParameterConfig[] getParameters() {
            return parameters;
        }

        public void setParameters(ParameterConfig[] parameters) {
            this.parameters = parameters;
        }

        public RequestConfig getRequest() {
            return request;
        }

        public void setRequest(RequestConfig request) {
            this.request = request;
        }

        public Map<String, ResponseConfig> getResponses() {
            return responses;
        }

        public void setResponses(Map<String, ResponseConfig> responses) {
            this.responses = responses;
        }
    }

    /**
     * 参数配置
     */
    public static class ParameterConfig {
        private String name;
        private boolean required;
        private SchemaType schema;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public SchemaType getSchema() {
            return schema;
        }

        public void setSchema(SchemaType schema) {
            this.schema = schema;
        }

        public static class SchemaType {
            private String type;
            private String format;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getFormat() {
                return format;
            }

            public void setFormat(String format) {
                this.format = format;
            }
        }
    }

    /**
     * 请求配置
     */
    public static class RequestConfig {
        private boolean required;
        private ContentConfig content;

        public boolean isRequired() {
            return required;
        }

        public void setRequired(boolean required) {
            this.required = required;
        }

        public ContentConfig getContent() {
            return content;
        }

        public void setContent(ContentConfig content) {
            this.content = content;
        }

        public static class ContentConfig {
            private ApplicationJsonConfig applicationJson;

            public ApplicationJsonConfig getApplicationJson() {
                return applicationJson;
            }

            public void setApplicationJson(ApplicationJsonConfig applicationJson) {
                this.applicationJson = applicationJson;
            }

            public static class ApplicationJsonConfig {
                private SchemaRef schema;

                public SchemaRef getSchema() {
                    return schema;
                }

                public void setSchema(SchemaRef schema) {
                    this.schema = schema;
                }

                public static class SchemaRef {
                    private String $ref;

                    public String get$ref() {
                        return $ref;
                    }

                    public void set$ref(String $ref) {
                        this.$ref = $ref;
                    }
                }
            }
        }
    }

    /**
     * 响应配置
     */
    public static class ResponseConfig {
        private String description;
        private ContentConfig content;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public ContentConfig getContent() {
            return content;
        }

        public void setContent(ContentConfig content) {
            this.content = content;
        }

        public static class ContentConfig {
            private ApplicationJsonConfig applicationJson;

            public ApplicationJsonConfig getApplicationJson() {
                return applicationJson;
            }

            public void setApplicationJson(ApplicationJsonConfig applicationJson) {
                this.applicationJson = applicationJson;
            }

            public static class ApplicationJsonConfig {
                private SchemaRef schema;
                private ParameterConfig[] parameters;

                public SchemaRef getSchema() {
                    return schema;
                }

                public void setSchema(SchemaRef schema) {
                    this.schema = schema;
                }

                public ParameterConfig[] getParameters() {
                    return parameters;
                }

                public void setParameters(ParameterConfig[] parameters) {
                    this.parameters = parameters;
                }

                public static class SchemaRef {
                    private String $ref;

                    public String get$ref() {
                        return $ref;
                    }

                    public void set$ref(String $ref) {
                        this.$ref = $ref;
                    }
                }
            }
        }
    }

    /**
     * Components配置
     */
    public static class Components {
        private Map<String, SchemaConfig> schemas;

        public Map<String, SchemaConfig> getSchemas() {
            return schemas;
        }

        public void setSchemas(Map<String, SchemaConfig> schemas) {
            this.schemas = schemas;
        }

        public static class SchemaConfig {
            private String type;
            private String description;
            private Map<String, PropertyConfig> properties;
            private String[] required;

            public String getType() {
                return type;
            }

            public void setType(String type) {
                this.type = type;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }

            public Map<String, PropertyConfig> getProperties() {
                return properties;
            }

            public void setProperties(Map<String, PropertyConfig> properties) {
                this.properties = properties;
            }

            public String[] getRequired() {
                return required;
            }

            public void setRequired(String[] required) {
                this.required = required;
            }

            public static class PropertyConfig {
                private String type;
                private String format;
                private Integer minLength;
                private Integer maxLength;

                public String getType() {
                    return type;
                }

                public void setType(String type) {
                    this.type = type;
                }

                public String getFormat() {
                    return format;
                }

                public void setFormat(String format) {
                    this.format = format;
                }

                public Integer getMinLength() {
                    return minLength;
                }

                public void setMinLength(Integer minLength) {
                    this.minLength = minLength;
                }

                public Integer getMaxLength() {
                    return maxLength;
                }

                public void setMaxLength(Integer maxLength) {
                    this.maxLength = maxLength;
                }
            }
        }
    }
}
