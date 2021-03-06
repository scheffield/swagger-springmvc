package com.mangofactory.swagger.controllers
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.mangofactory.swagger.configuration.JacksonScalaSupport
import com.mangofactory.swagger.core.SwaggerApiResourceListing
import com.mangofactory.swagger.core.SwaggerCache
import com.mangofactory.swagger.mixins.ApiListingSupport
import com.mangofactory.swagger.mixins.AuthSupport
import com.wordnik.swagger.model.AuthorizationType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.web.servlet.View
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.*

@Mixin([com.mangofactory.swagger.mixins.JsonSupport, ApiListingSupport, AuthSupport])
class DefaultSwaggerControllerSpec extends Specification {

  @Shared
  MockMvc mockMvc
  @Shared
  View mockView
  @Shared
  DefaultSwaggerController controller = new DefaultSwaggerController()

  def setup() {
    def jackson2 = new MappingJackson2HttpMessageConverter()

    JacksonScalaSupport jacksonScalaSupport = new JacksonScalaSupport()
    ObjectMapper mapper = new ObjectMapper()
    mapper.registerModule(new DefaultScalaModule())
    mapper.registerModule(jacksonScalaSupport.swaggerSerializationModule())

    jackson2.setObjectMapper(mapper)
    mockMvc = standaloneSetup(controller)
        .setSingleView(mockView)
        .setMessageConverters(jackson2)
        .build();
  }

  @Unroll("path: #path")
  def "should return the default or first swagger resource listing"() {
  given:
    SwaggerCache swaggerCache = new SwaggerCache();
    SwaggerApiResourceListing swaggerApiResourceListing = new SwaggerApiResourceListing(swaggerCache, "")
    swaggerApiResourceListing.initialize()
    controller.swaggerCache = swaggerCache


  when:
    MvcResult result = mockMvc.perform(get(path)).andDo(print()).andReturn()
    def responseJson = jsonBodyResponse(result)
  then:
    result.getResponse().getStatus() == expectedStatus
  where:
    path                | expectedStatus
    "/api-docs"         | 200
    "/api-docs/"        | 200
    "/api-docs/unknown" | 404
  }

  def "should respond with api listing for a given resource group"() {
  given:
    SwaggerCache swaggerCache = new SwaggerCache();
    swaggerCache.swaggerApiListingMap = ['businesses': apiListing()]
    controller.swaggerCache = swaggerCache
  when:
    MvcResult result = mockMvc.perform(get("/api-docs/businesses")).andDo(print()).andReturn()
    def responseJson = jsonBodyResponse(result)

  then:
    result.getResponse().getStatus() == 200

  }
  
  def "should respond with api listing for a given multi segment resource group"() {
    given:
      SwaggerCache swaggerCache = new SwaggerCache();
      swaggerCache.swaggerApiListingMap = ['api/v1/businesses': apiListing()]
      swaggerCache.swaggerApiResourceListingMap = ['some-swagger-group': resourceListing()]
      controller.swaggerCache = swaggerCache
        
    when:
      MvcResult result = mockMvc.perform(get("/api-docs/api/v1/businesses")).andDo(print()).andReturn()
      def responseJson = jsonBodyResponse(result)
      
    then:
      result.getResponse().getStatus() == 200
  
  }

  def "should respond with api listing for a given resource group prefixed with a swagger group"() {
    given:
    SwaggerCache swaggerCache = new SwaggerCache();
    swaggerCache.swaggerApiListingMap = ['businesses': apiListing()]
    swaggerCache.swaggerApiResourceListingMap = ['some-swagger-group': resourceListing()]
    controller.swaggerCache = swaggerCache
    when:
    MvcResult result = mockMvc.perform(get("/api-docs/some-swagger-group/businesses")).andDo(print()).andReturn()
    def responseJson = jsonBodyResponse(result)

    then:
    result.getResponse().getStatus() == 200

  }

  def "should respond with api listing for a given multi segment resource group prefixed with a swagger group"() {
	given:
	  SwaggerCache swaggerCache = new SwaggerCache();
	  swaggerCache.swaggerApiListingMap = ['api/v1/businesses': apiListing()]
      swaggerCache.swaggerApiResourceListingMap = ['some-swagger-group': resourceListing()]
	  controller.swaggerCache = swaggerCache
	  
	when:
	  MvcResult result = mockMvc.perform(get("/api-docs/some-swagger-group/api/v1/businesses")).andDo(print()).andReturn()
	  def responseJson = jsonBodyResponse(result)

	then:
	  result.getResponse().getStatus() == 200

  }
  
  def "should respond with auth included"() {
  given:
    SwaggerCache swaggerCache = new SwaggerCache();

    def authTypes = new ArrayList<AuthorizationType>()

    authTypes.add(authorizationTypes());

    swaggerCache.swaggerApiResourceListingMap = [resourceKey: resourceListing(authTypes)]
    controller.swaggerCache = swaggerCache
  when:
    MvcResult result = mockMvc.perform(get("/api-docs")).andDo(print()).andReturn()
    def json = jsonBodyResponse(result)

  then:
    result.getResponse().getStatus() == 200
    assertDefaultAuth(json)
  }


}