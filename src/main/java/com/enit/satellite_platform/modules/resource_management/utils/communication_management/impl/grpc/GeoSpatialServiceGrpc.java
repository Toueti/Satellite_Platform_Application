package com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: geospatial.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class GeoSpatialServiceGrpc {

  private GeoSpatialServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "com.enit.satellite_platform.grpc.GeoSpatialService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest,
      com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse> getProcessDataMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ProcessData",
      requestType = com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest.class,
      responseType = com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest,
      com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse> getProcessDataMethod() {
    io.grpc.MethodDescriptor<com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest, com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse> getProcessDataMethod;
    if ((getProcessDataMethod = GeoSpatialServiceGrpc.getProcessDataMethod) == null) {
      synchronized (GeoSpatialServiceGrpc.class) {
        if ((getProcessDataMethod = GeoSpatialServiceGrpc.getProcessDataMethod) == null) {
          GeoSpatialServiceGrpc.getProcessDataMethod = getProcessDataMethod =
              io.grpc.MethodDescriptor.<com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest, com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ProcessData"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse.getDefaultInstance()))
              .setSchemaDescriptor(new GeoSpatialServiceMethodDescriptorSupplier("ProcessData"))
              .build();
        }
      }
    }
    return getProcessDataMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GeoSpatialServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GeoSpatialServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GeoSpatialServiceStub>() {
        @java.lang.Override
        public GeoSpatialServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GeoSpatialServiceStub(channel, callOptions);
        }
      };
    return GeoSpatialServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GeoSpatialServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GeoSpatialServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GeoSpatialServiceBlockingStub>() {
        @java.lang.Override
        public GeoSpatialServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GeoSpatialServiceBlockingStub(channel, callOptions);
        }
      };
    return GeoSpatialServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GeoSpatialServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GeoSpatialServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GeoSpatialServiceFutureStub>() {
        @java.lang.Override
        public GeoSpatialServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GeoSpatialServiceFutureStub(channel, callOptions);
        }
      };
    return GeoSpatialServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     */
    default void processData(com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest request,
        io.grpc.stub.StreamObserver<com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getProcessDataMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service GeoSpatialService.
   */
  public static abstract class GeoSpatialServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return GeoSpatialServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service GeoSpatialService.
   */
  public static final class GeoSpatialServiceStub
      extends io.grpc.stub.AbstractAsyncStub<GeoSpatialServiceStub> {
    private GeoSpatialServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GeoSpatialServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GeoSpatialServiceStub(channel, callOptions);
    }

    /**
     */
    public void processData(com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest request,
        io.grpc.stub.StreamObserver<com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getProcessDataMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service GeoSpatialService.
   */
  public static final class GeoSpatialServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<GeoSpatialServiceBlockingStub> {
    private GeoSpatialServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GeoSpatialServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GeoSpatialServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse processData(com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getProcessDataMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service GeoSpatialService.
   */
  public static final class GeoSpatialServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<GeoSpatialServiceFutureStub> {
    private GeoSpatialServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GeoSpatialServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GeoSpatialServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse> processData(
        com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getProcessDataMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PROCESS_DATA = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PROCESS_DATA:
          serviceImpl.processData((com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest) request,
              (io.grpc.stub.StreamObserver<com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getProcessDataMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadRequest,
              com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.PayloadResponse>(
                service, METHODID_PROCESS_DATA)))
        .build();
  }

  private static abstract class GeoSpatialServiceBaseDescriptorSupplier
      implements  io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GeoSpatialServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.enit.satellite_platform.modules.resource_management.utils.communication_management.impl.grpc.GeoSpatialProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("GeoSpatialService");
    }
  }

  private static final class GeoSpatialServiceFileDescriptorSupplier
      extends GeoSpatialServiceBaseDescriptorSupplier {
    GeoSpatialServiceFileDescriptorSupplier() {}
  }

  private static final class GeoSpatialServiceMethodDescriptorSupplier
      extends GeoSpatialServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    GeoSpatialServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (GeoSpatialServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GeoSpatialServiceFileDescriptorSupplier())
              .addMethod(getProcessDataMethod())
              .build();
        }
      }
    }
    return result;
  }
}
