package org.tio.core;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.tio.core.intf.Packet;
import org.tio.core.task.DecodeRunnable;
import org.tio.core.utils.AioUtils;

/**
 * 
 * @author tanyaowu 
 * @创建时间 2016年11月15日 下午1:31:04
 *
 * @操作列表
 *  编号	| 操作时间	| 操作人员	 | 操作说明
 *  (1) | 2016年11月15日 | tanyaowu | 新建类
 *
 */
public class ReadCompletionHandler<SessionContext, P extends Packet, R> implements CompletionHandler<Integer, ByteBuffer>
{

	private static Logger log = LoggerFactory.getLogger(ReadCompletionHandler.class);
	private ChannelContext<SessionContext, P, R> channelContext = null;
	private ByteBuffer readByteBuffer;

	//	private ByteBuffer byteBuffer = ByteBuffer.allocate(ChannelContext.READ_BUFFER_SIZE);

	/**
	 * 
	 *
	 * @author: tanyaowu
	 * @创建时间:　2016年11月15日 下午1:31:04
	 * 
	 */
	public ReadCompletionHandler(ChannelContext<SessionContext, P, R> channelContext)
	{
		this.setChannelContext(channelContext);
		this.readByteBuffer = ByteBuffer.allocate(channelContext.getGroupContext().getReadBufferSize());
	}

	@Override
	public void completed(Integer result, ByteBuffer byteBuffer)
	{
//		GroupContext<SessionContext, P, R> groupContext = channelContext.getGroupContext();
		if (result > 0)
		{
//			ByteBuffer newByteBuffer = ByteBufferUtils.copy(readByteBuffer, 0, readByteBuffer.position());
			DecodeRunnable<SessionContext, P, R> decodeRunnable = channelContext.getDecodeRunnable();
			readByteBuffer.flip();
			decodeRunnable.setNewByteBuffer(readByteBuffer);
			decodeRunnable.run();
//			decodeRunnable.addMsg(newByteBuffer);
//			groupContext.getDecodeExecutor().execute(decodeRunnable);
		} else if (result == 0)
		{
			log.error("{}读到的数据长度为0", channelContext);
		} else if (result < 0)
		{
			Aio.close(channelContext, null, "读数据时返回" + result);
		}

		if (AioUtils.checkBeforeIO(channelContext))
		{
			AsynchronousSocketChannel asynchronousSocketChannel = channelContext.getAsynchronousSocketChannel();
			readByteBuffer.position(0);
			readByteBuffer.limit(readByteBuffer.capacity());
			asynchronousSocketChannel.read(readByteBuffer, readByteBuffer, this);
		}

	}

	/** 
	 * @see java.nio.channels.CompletionHandler#failed(java.lang.Throwable, java.lang.Object)
	 * 
	 * @param exc
	 * @param attachment
	 * @重写人: tanyaowu
	 * @重写时间: 2016年11月16日 下午1:40:59
	 * 
	 */
	@Override
	public void failed(Throwable exc, ByteBuffer byteBuffer)
	{
		Aio.close(channelContext, exc, "读数据时发生异常");

	}

	/**
	 * @return the channelContext
	 */
	public ChannelContext<SessionContext, P, R> getChannelContext()
	{
		return channelContext;
	}

	/**
	 * @param channelContext the channelContext to set
	 */
	public void setChannelContext(ChannelContext<SessionContext, P, R> channelContext)
	{
		this.channelContext = channelContext;
	}

	/**
	 * @return the readByteBuffer
	 */
	public ByteBuffer getReadByteBuffer()
	{
		return readByteBuffer;
	}

}
