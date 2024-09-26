# AndroidCamera

本项目主要涉及Android开发中Camera的相关操作、预览方式、视频录制等。项目结构简单代码耦合性低，适合学习和使用

<p align = "center">    
<img  src="./img/index.jpg" height="480" />
  <img  src="./img/camera.jpg" height="480" />
</p>



## lib-camera
|包|说明  |
|--|--|
| camera | camera相关操作功能包，包括Camera和Camera2。以及各种预览视图 |
| encoder | MediaCdoec录制视频相关，包括对ByteBuffer和Surface的录制 |
| gles | opengles操作相关 |
| permission | 权限相关 |
| util | 工具类 |

每个包都可独立使用，耦合度低，方便白嫖：）

## 预览视图

| 类名                       | 功能说明                                                     |
| -------------------------- | ------------------------------------------------------------ |
| CameraSurfaceView          | 1.SurfaceView+Camera预览<br>2.包含MediaCodec+Buffer录制视频  |
| CameraTextureView          | TextureView+Camera                                           |
| CameraGLSurfaceView        | 1.GLSurfaceView+Camera<br>2.MediaCodec+Surface录制视频，多线程共享EGL方式 |
| CameraGLSurfaceHolderView  | 1.SurfaceView+OpenGL ES+Camera<br>2.MediaCodec+Surface录制视频，三种绘制方式：**Draw Twice**、**Draw FBO**、**Draw Blit framebuffer** |
| CameraGLTextureView        | 1.TextureView+OpenGL ES+Camera<br/>2.MediaCodec+Surface录制视频，三种绘制方式：**Draw Twice**、**Draw FBO**、**Draw Blit framebuffer** |
| Camera2SurfaceView         | SurfaceView+Camera2                                          |
| Camera2TextureView         | TextureView+Camera2                                          |
| Camera2GLSurfaceView       | 1.GLSurfaceView+Camera2<br/>2.MediaCodec+Surface录制视频，多线程共享EGL方式 |
| Camera2GLSurfaceHolderView | 1.SurfaceView+OpenGL ES+Camera2<br/>2.MediaCodec+Surface录制视频，三种绘制方式：**Draw Twice**、**Draw FBO**、**Draw Blit framebuffer** |
| Camera2GLTextureView       | 1.TextureView+OpenGL ES+Camera2<br/>2.MediaCodec+Surface录制视频，三种绘制方式：**Draw Twice**、**Draw FBO**、**Draw Blit framebuffer** |



## FAQ

### 1.如何切换预览尺寸

`ICameraManager`提供了`setPreviewSize(Size size)`接口可以在openCamera之前设置想要的预览尺寸

### 2.如何获取预览帧数据

`ICameraManager`提供了`addPreviewBufferCallback(PreviewBufferCallback previewBufferCallback)`接口可以在回调中获取Camera预览数据，格式为**NV21**

> 注意：获取的byte[]是可复用的，需要您自行arrayCopy一份使用

### 3.如何拍照

`ICameraManager`提供了`takePicture(PictureBufferCallback pictureCallback)`接口可以在回调中获取拍照数据，格式为**JPG**

## Blog

[Android Camera系列（一）：SurfaceView+Camera](https://blog.csdn.net/xiaozhiwz/article/details/141472537)

[Android Camera系列（二）：TextureView+Camera](https://blog.csdn.net/xiaozhiwz/article/details/141855031)

[Android Camera系列（三）：GLSurfaceView+Camera](https://blog.csdn.net/xiaozhiwz/article/details/141860162)



参考：
1. [https://github.com/afei-cn/CameraDemo](https://github.com/afei-cn/CameraDemo)
2. [https://github.com/saki4510t/UVCCamera](https://github.com/saki4510t/UVCCamera)
3. [https://github.com/google/grafika](https://github.com/google/grafika)