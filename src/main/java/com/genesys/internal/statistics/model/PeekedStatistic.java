/*
 * Statistics Service
 * Statistics Service
 *
 * OpenAPI spec version: 9.0.000.21.527
 * 
 *
 * NOTE: This class is auto generated by the swagger code generator program.
 * https://github.com/swagger-api/swagger-codegen.git
 * Do not edit the class manually.
 */


package com.genesys.internal.statistics.model;

import java.util.Objects;
import com.genesys.internal.statistics.model.PeekedStatisticValue;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.IOException;

/**
 * PeekedStatistic
 */
@javax.annotation.Generated(value = "io.swagger.codegen.languages.JavaClientCodegen", date = "2018-11-28T17:52:33.440Z")
public class PeekedStatistic {
  @SerializedName("statistic")
  private PeekedStatisticValue statistic = null;

  public PeekedStatistic statistic(PeekedStatisticValue statistic) {
    this.statistic = statistic;
    return this;
  }

   /**
   * Get statistic
   * @return statistic
  **/
  @ApiModelProperty(required = true, value = "")
  public PeekedStatisticValue getStatistic() {
    return statistic;
  }

  public void setStatistic(PeekedStatisticValue statistic) {
    this.statistic = statistic;
  }


  @Override
  public boolean equals(java.lang.Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PeekedStatistic peekedStatistic = (PeekedStatistic) o;
    return Objects.equals(this.statistic, peekedStatistic.statistic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(statistic);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PeekedStatistic {\n");
    
    sb.append("    statistic: ").append(toIndentedString(statistic)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(java.lang.Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}

