package io.graphoenix.core.dto.inputObjectType;

import com.dslplatform.json.CompiledJson;
import io.graphoenix.spi.annotation.Ignore;
import jakarta.annotation.Generated;
import java.lang.String;
import java.util.Collection;
import org.eclipse.microprofile.graphql.Input;

@CompiledJson
@Input
@Generated("io.graphoenix.java.generator.builder.TypeSpecBuilderProxy")
@Ignore
public class Invoke {
  private String className;

  private String methodName;

  private Collection<InvokeParameter> parameters;

  private String returnClassName;

  private Collection<String> thrownTypes;

  public String getClassName() {
    return this.className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getMethodName() {
    return this.methodName;
  }

  public void setMethodName(String methodName) {
    this.methodName = methodName;
  }

  public Collection<InvokeParameter> getParameters() {
    return this.parameters;
  }

  public void setParameters(Collection<InvokeParameter> parameters) {
    this.parameters = parameters;
  }

  public String getReturnClassName() {
    return this.returnClassName;
  }

  public void setReturnClassName(String returnClassName) {
    this.returnClassName = returnClassName;
  }

  public Collection<String> getThrownTypes() {
    return this.thrownTypes;
  }

  public void setThrownTypes(Collection<String> thrownTypes) {
    this.thrownTypes = thrownTypes;
  }
}
