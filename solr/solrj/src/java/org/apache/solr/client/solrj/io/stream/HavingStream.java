/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.solr.client.solrj.io.stream;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.comp.StreamComparator;
import org.apache.solr.client.solrj.io.ops.BooleanOperation;
import org.apache.solr.client.solrj.io.ops.StreamOperation;
import org.apache.solr.client.solrj.io.stream.expr.Explanation;
import org.apache.solr.client.solrj.io.stream.expr.Explanation.ExpressionType;
import org.apache.solr.client.solrj.io.stream.expr.Expressible;
import org.apache.solr.client.solrj.io.stream.expr.StreamExplanation;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;

/**
 * The HavingStream iterates over an internal stream and applies a BooleanOperation to each tuple. If the BooleanOperation
 * evaluates to true then the HavingStream emits the tuple, if it returns false the tuple is not emitted.
 **/

public class HavingStream extends TupleStream implements Expressible {

  private static final long serialVersionUID = 1;

  private TupleStream stream;
  private BooleanOperation op;

  private transient Tuple currentGroupHead;

  public HavingStream(TupleStream stream, BooleanOperation op) throws IOException {
    init(stream, op);
  }


  public HavingStream(StreamExpression expression, StreamFactory factory) throws IOException{
    // grab all parameters out
    List<StreamExpression> streamExpressions = factory.getExpressionOperandsRepresentingTypes(expression, Expressible.class, TupleStream.class);
    List<StreamExpression> operationExpressions = factory.getExpressionOperandsRepresentingTypes(expression, BooleanOperation.class);

    // validate expression contains only what we want.
    if(expression.getParameters().size() != streamExpressions.size() + 1){
      throw new IOException(String.format(Locale.ROOT,"Invalid expression %s - unknown operands found", expression));
    }

    if(1 != streamExpressions.size()){
      throw new IOException(String.format(Locale.ROOT,"Invalid expression %s - expecting a single stream but found %d",expression, streamExpressions.size()));
    }


    BooleanOperation booleanOperation = null;
    if(operationExpressions != null && operationExpressions.size() == 1) {
      StreamExpression ex = operationExpressions.get(0);
      StreamOperation operation = factory.constructOperation(ex);
      if(operation instanceof BooleanOperation) {
        booleanOperation = (BooleanOperation) operation;
      } else {
        throw new IOException("The HavingStream requires a BooleanOperation. A StreamOperation was provided.");
      }
    } else {
      throw new IOException("The HavingStream requires a BooleanOperation.");
    }

    init(factory.constructStream(streamExpressions.get(0)), booleanOperation);
  }

  private void init(TupleStream stream, BooleanOperation op) throws IOException{
    this.stream = stream;
    this.op = op;
  }

  @Override
  public StreamExpression toExpression(StreamFactory factory) throws IOException{
    return toExpression(factory, true);
  }

  private StreamExpression toExpression(StreamFactory factory, boolean includeStreams) throws IOException {
    // function name
    StreamExpression expression = new StreamExpression(factory.getFunctionName(this.getClass()));

    // stream
    if(includeStreams){
      expression.addParameter(((Expressible) stream).toExpression(factory));
    }
    else{
      expression.addParameter("<stream>");
    }

    if(op instanceof Expressible) {
      expression.addParameter(op.toExpression(factory));
    } else {
      throw new IOException("This ReducerStream contains a non-expressible operation - it cannot be converted to an expression");
    }

    return expression;
  }

  @Override
  public Explanation toExplanation(StreamFactory factory) throws IOException {

    return new StreamExplanation(getStreamNodeId().toString())
        .withChildren(new Explanation[]{
            stream.toExplanation(factory)
        })
        .withFunctionName(factory.getFunctionName(this.getClass()))
        .withImplementingClass(this.getClass().getName())
        .withExpressionType(ExpressionType.STREAM_DECORATOR)
        .withExpression(toExpression(factory, false).toString())
        .withHelpers(new Explanation[]{
            op.toExplanation(factory)
        });
  }

  public void setStreamContext(StreamContext context) {
    this.stream.setStreamContext(context);
  }

  public List<TupleStream> children() {
    List<TupleStream> l =  new ArrayList<TupleStream>();
    l.add(stream);
    return l;
  }

  public void open() throws IOException {
    stream.open();
  }

  public void close() throws IOException {
    stream.close();
  }

  public Tuple read() throws IOException {
    while(true) {
      Tuple tuple = stream.read();
      if(tuple.EOF) {
        return tuple;
      }

      op.operate(tuple);

      if(op.evaluate()) {
        return tuple;
      }
    }
  }

  /** Return the stream sort - ie, the order in which records are returned */
  public StreamComparator getStreamSort(){
    return stream.getStreamSort();
  }

  public int getCost() {
    return 0;
  }
}