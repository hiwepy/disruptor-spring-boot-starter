# spring-boot-starter-disruptor

starter for disruptor

### 基于 Disruptor 的 Spring Boot Starter 实现, 异步事件推送、处理封装

  - 1、事件推送

    a、配置简单，少量配置即可实现异步事件推送

  -  2、事件处理

    a、配置简单，少量配置即可实现异步事件处理

    b、组件实现了基于责任链的事件处理实现；可实现对具备不同 事件规则 ruleExpression  的事件对象进行专责处理；就如 Filter，该组件实现的Handler采用了同样的原理；


   - /Event-DC-Output/TagA-Output/** = inDbPostHandler  该配置表示；Event = Event-DC-Output , Tags = TagA-Output , Keys = 任何类型 的事件对象交由 inDbPostHandler  来处理
   - /Event-DC-Output/TagB-Output/** = smsPostHandler  该配置表示；Event = Event-DC-Output , Tags = TagB-Output , Keys = 任何类型 的事件对象交由 smsPostHandler 来处理

    通过这种责任链的机制，很好的实现了事件的分类异步处理；比如消息队列的消费端需要快速的消费各类消息，且每种处理实现都不相同；这时候就需要用到事件对象的分类异步处理。

### Maven

``` xml
<dependency>
	<groupId>${project.groupId}</groupId>
	<artifactId>spring-boot-starter-disruptor</artifactId>
	<version>1.0.2.RELEASE</version>
</dependency>
```

### Sample

[https://github.com/vindell/spring-boot-starter-samples/tree/master/spring-boot-sample-disruptor](https://github.com/vindell/spring-boot-starter-samples/tree/master/spring-boot-sample-disruptor "spring-boot-sample-disruptor")
