package org.tio.core;

import java.nio.ByteBuffer;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.locks.Lock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tio.core.intf.Packet;
import org.tio.core.maintain.ChannelContextMapWithLock;
import org.tio.core.task.SendRunnable;
import org.tio.core.threadpool.SynThreadPoolExecutor;
import org.tio.core.threadpool.intf.SynRunnableIntf;
import org.tio.core.utils.AioUtils;
import org.tio.core.utils.ThreadUtils;

/**
 * The Class Aio. t-io用户关心的API几乎全在这
 *
 * @author tanyaowu
 */
public class Aio
{

	/** The log. */
	private static Logger log = LoggerFactory.getLogger(Aio.class);

	/**
	 * Instantiates a new aio.
	 *
	 * @author: tanyaowu
	 */
	private Aio(){}

	/**
	 * 根据clientip和clientport获取ChannelContext
	 * @param groupContext
	 * @param clientIp
	 * @param clientPort
	 * @return
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> ChannelContext<SessionContext, P, R> getChannelContextByClientNode(GroupContext<SessionContext, P, R> groupContext,
			String clientIp, Integer clientPort)
	{
		return groupContext.getClientNodes().find(clientIp, clientPort);
	}

	/**
	 * 一个组有哪些客户端
	 *
	 * @param groupid the groupid
	 * @return the obj with read write lock
	 */
	public static <SessionContext, P extends Packet, R> ObjWithLock<Set<ChannelContext<SessionContext, P, R>>> getChannelContextsByGroup(
			GroupContext<SessionContext, P, R> groupContext, String groupid)
	{
		return groupContext.getGroups().clients(groupid);
	}

	

	/**
	 * 绑定群组
	 * @param channelContext
	 * @param groupid
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> void bindGroup(ChannelContext<SessionContext, P, R> channelContext, String groupid)
	{
		channelContext.getGroupContext().getGroups().bind(groupid, channelContext);
	}

	/**
	 * 与所有组解除解绑关系
	 * @param groupContext
	 * @param channelContext
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> void unbindGroup(ChannelContext<SessionContext, P, R> channelContext)
	{
		channelContext.getGroupContext().getGroups().unbind(channelContext);
	}
	
	/**
	 * 与指定组解除绑定关系
	 * @param group
	 * @param channelContext
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> void unbindGroup(String group, ChannelContext<SessionContext, P, R> channelContext)
	{
		channelContext.getGroupContext().getGroups().unbind(group, channelContext);
	}
	
	/**
	 * 绑定用户
	 * @param groupContext
	 * @param channelContext
	 * @param userid
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> void bindUser(ChannelContext<SessionContext, P, R> channelContext, String userid)
	{
		channelContext.getGroupContext().getUsers().bind(userid, channelContext);
	}

	/**
	 * 解绑用户
	 * @param groupContext
	 * @param channelContext
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> void unbindUser(ChannelContext<SessionContext, P, R> channelContext)
	{
		channelContext.getGroupContext().getUsers().unbind(channelContext);
	}

	/**
	 * 根据userid获取ChannelContext
	 * @param groupContext
	 * @param userid
	 * @return
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> ChannelContext<SessionContext, P, R> getChannelContextByUserid(GroupContext<SessionContext, P, R> groupContext,
			String userid)
	{
		return groupContext.getUsers().find(userid);
	}

	/**
	 * 发消息给指定用户
	 * @param groupContext
	 * @param userid
	 * @param packet
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> void sendToUser(GroupContext<SessionContext, P, R> groupContext, String userid, P packet)
	{
		ChannelContext<SessionContext, P, R> channelContext = groupContext.getUsers().find(userid);
		send(channelContext, packet);
	}

	/**
	 * 发消息到指定channelContext
	 *
	 * @param <Ext> the generic type
	 * @param <P> the generic type
	 * @param <R> the generic type
	 * @param channelContext the channel context
	 * @param packet the packet
	 */
	public static <SessionContext, P extends Packet, R> void send(ChannelContext<SessionContext, P, R> channelContext, P packet)
	{
		if (channelContext == null)
		{
			log.error("channelContext == null");
			return;
		}
		if (channelContext.isClosed() || channelContext.isRemoved())
		{
			log.error("{}, isClosed:{}, isRemoved:{}, stack:{} ", channelContext, channelContext.isClosed(), channelContext.isRemoved(), ThreadUtils.stackTrace());
			return;
		}
		SendRunnable<SessionContext, P, R> sendRunnable = AioUtils.selectSendRunnable(channelContext, packet);
		sendRunnable.addMsg(packet);
		SynThreadPoolExecutor<SynRunnableIntf> synThreadPoolExecutor = AioUtils.selectSendExecutor(channelContext, packet);
		synThreadPoolExecutor.execute(sendRunnable);
	}

	/**
	 * 发送到指定的ip和port
	 * @param groupContext
	 * @param ip
	 * @param port
	 * @param packet
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> void send(GroupContext<SessionContext, P, R> groupContext, String ip, int port, P packet)
	{
		ChannelContext<SessionContext, P, R> channelContext = groupContext.getClientNodes().find(ip, port);
		if (channelContext != null)
		{
			send(channelContext, packet);
		} else
		{
			log.warn("can find channelContext by {}:{}", ip, port);
		}
	}

	/**
	 * 发消息到组
	 * @param groupContext
	 * @param groupid
	 * @param packet
	 * @param channelContextFilter filter方法返回为false的ChannelContext将会过滤掉(即不会把消息发给这个ChannelContext)
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> void sendToGroup(GroupContext<SessionContext, P, R> groupContext, String groupid, P packet,
			ChannelContextFilter<SessionContext, P, R> channelContextFilter)
	{
		ObjWithLock<Set<ChannelContext<SessionContext, P, R>>> setWithLock = groupContext.getGroups().clients(groupid);
		if (setWithLock == null)
		{
			log.error("组[{}]不存在", groupid);
			return;
		}

		sendToSet(groupContext, setWithLock, packet, channelContextFilter);
	}

	/**
	 * 发消息到组
	 * @param groupContext
	 * @param groupid
	 * @param packet
	 *
	 * @author: tanyaowu
	 *
	 */
	public static <SessionContext, P extends Packet, R> void sendToGroup(GroupContext<SessionContext, P, R> groupContext, String groupid, P packet)
	{
		sendToGroup(groupContext, groupid, packet, null);
	}

	/**
	 * 发消息到所有连接
	 * @param groupContext
	 * @param packet
	 * @param channelContextFilter
	 *
	 * @author: tanyaowu
	 * 2017年3月21日 上午9:12:48
	 *
	 */
	public static <SessionContext, P extends Packet, R> void sendToAll(GroupContext<SessionContext, P, R> groupContext, P packet,
			ChannelContextFilter<SessionContext, P, R> channelContextFilter)
	{
		ObjWithLock<Set<ChannelContext<SessionContext, P, R>>> setWithLock = groupContext.getConnections().getSetWithLock();
		if (setWithLock == null)
		{
			log.debug("没有任何连接");
			return;
		}

		sendToSet(groupContext, setWithLock, packet, channelContextFilter);
	}

	/**
	 * 发消息到指定集合
	 * @param groupContext
	 * @param setWithLock
	 * @param packet
	 * @param channelContextFilter
	 *
	 * @author: tanyaowu
	 * 2017年2月5日 上午9:10:55
	 *
	 */
	public static <SessionContext, P extends Packet, R> void sendToSet(GroupContext<SessionContext, P, R> groupContext,
			ObjWithLock<Set<ChannelContext<SessionContext, P, R>>> setWithLock, P packet, ChannelContextFilter<SessionContext, P, R> channelContextFilter)
	{
		Lock lock = setWithLock.getLock().readLock();
		try
		{
			lock.lock();
			Set<ChannelContext<SessionContext, P, R>> set = setWithLock.getObj();
			if (set.size() == 0)
			{
				log.debug("集合为空");
				return;
			}
			if (!groupContext.isEncodeCareWithChannelContext())
			{
				ByteBuffer byteBuffer = groupContext.getAioHandler().encode(packet, groupContext, null);
				packet.setPreEncodedByteBuffer(byteBuffer);
			}

			for (ChannelContext<SessionContext, P, R> channelContext : set)
			{
				if (channelContextFilter != null)
				{
					boolean isfilter = channelContextFilter.filter(channelContext);
					if (!isfilter)
					{
						continue;
					}
				}
				send(channelContext, packet);
			}
		} catch (Exception e)
		{
			log.error(e.toString(), e);
		} finally
		{
			lock.unlock();
		}
	}

	/**
	 * 同步发送消息.<br>
	 * 注意：<br>
	 * 1、参数packet的synSeq不为空且大于0（null、等于小于0都不行）<br>
	 * 2、对端收到此消息后，需要回一条synSeq一样的消息<br>
	 * 3、对于同步发送，框架层面并不会帮应用去调用handler.handler(packet, channelContext)方法，应用需要自己去处理响应的消息包，参考：groupContext.getAioHandler().handler(packet, channelContext);<br>
	 * @param channelContext
	 * @param packet
	 * @param timeout 超时时间，单位：毫秒
	 * @return
	 *
	 * @author: tanyaowu
	 * 2017年1月1日 下午12:52:11
	 *
	 */
	@SuppressWarnings("finally")
	public static <SessionContext, P extends Packet, R> P synSend(ChannelContext<SessionContext, P, R> channelContext, P packet, long timeout)
	{
		if (channelContext == null)
		{
			throw new RuntimeException("channelContext == null");
		}

		Integer synSeq = packet.getSynSeq();
		if (synSeq == null || synSeq <= 0)
		{
			throw new RuntimeException("synSeq必须大于0");
		}

		ChannelContextMapWithLock<SessionContext, P, R> syns = channelContext.getGroupContext().getSyns();
		try
		{
			syns.put(synSeq, packet);

			synchronized (packet)
			{
				send(channelContext, packet);
				try
				{
					packet.wait(timeout);
				} catch (InterruptedException e)
				{
					log.error(e.toString(), e);
				}
			}
		} catch (Exception e)
		{
			log.error(e.toString(), e);
		} finally
		{
			P respPacket = syns.remove(synSeq);
			if (respPacket == null)
			{
				log.error("respPacket == null,{}", channelContext);
				return null;
			}
			if (respPacket == packet)
			{
				log.error("同步发送超时,{}", channelContext);
				return null;
			}
			return respPacket;
		}
	}
	
	private static <SessionContext, P extends Packet, R> void close(ChannelContext<SessionContext, P, R> channelContext, Throwable throwable, String remark, boolean isNeedRemove)
	{
		if (channelContext == null)
		{
			log.error("channelContext == null");
			return;
		}
		
		if (channelContext.isWaitingClose())
		{
			log.info("{} 正在等待被关闭", channelContext);
			return;
		}
		
		synchronized (channelContext)
		{
			channelContext.setWaitingClose(true);
			ThreadPoolExecutor closePoolExecutor = channelContext.getGroupContext().getClosePoolExecutor();
			closePoolExecutor.execute(new CloseRunnable<>(channelContext, throwable, remark, isNeedRemove));
		}
		
	}

	/**
	 * Close.
	 *
	 * @param <Ext> the generic type
	 * @param <P> the generic type
	 * @param <R> the generic type
	 * @param channelContext the channel context
	 * @param throwable the t
	 * @param remark the remark
	 */
	public static <SessionContext, P extends Packet, R> void close(ChannelContext<SessionContext, P, R> channelContext, Throwable throwable, String remark)
	{
		close(channelContext, throwable, remark, false);
	}

	/**
	 * 和close方法一样，只不过不再进行重连等维护性的操作
	 * @param channelContext
	 * @param throwable
	 * @param remark
	 *
	 * @author: tanyaowu
	 * 2017年1月11日 下午7:53:19
	 *
	 */
	public static <SessionContext, P extends Packet, R> void remove(ChannelContext<SessionContext, P, R> channelContext, Throwable throwable, String remark)
	{
		close(channelContext, throwable, remark, true);
	}

	/**
	 * 
	 * @param channelContext
	 * @param remark
	 *
	 * @author: tanyaowu
	 * 2017年1月11日 下午7:53:48
	 *
	 */
	public static <SessionContext, P extends Packet, R> void close(ChannelContext<SessionContext, P, R> channelContext, String remark)
	{
		close(channelContext, null, remark);
	}

	/**
	 * 和close方法一样，只不过不再进行重连等维护性的操作
	 * @param channelContext
	 * @param remark
	 *
	 * @author: tanyaowu
	 * 2017年1月11日 下午7:53:53
	 *
	 */
	public static <SessionContext, P extends Packet, R> void remove(ChannelContext<SessionContext, P, R> channelContext, String remark)
	{
		remove(channelContext, null, remark);
	}

	/**
	 * 
	 * @param groupContext
	 * @param clientIp
	 * @param clientPort
	 * @param throwable
	 * @param remark
	 *
	 * @author: tanyaowu
	 * 2017年2月4日 下午1:38:32
	 *
	 */
	public static <SessionContext, P extends Packet, R> void close(GroupContext<SessionContext, P, R> groupContext, String clientIp, Integer clientPort, Throwable throwable,
			String remark)
	{
		ChannelContext<SessionContext, P, R> channelContext = groupContext.getClientNodes().find(clientIp, clientPort);
		close(channelContext, throwable, remark);
	}

	/**
	 * 和close方法一样，只不过不再进行重连等维护性的操作
	 * @param groupContext
	 * @param clientIp
	 * @param clientPort
	 * @param throwable
	 * @param remark
	 *
	 * @author: tanyaowu
	 * 2017年1月11日 下午7:54:03
	 *
	 */
	public static <SessionContext, P extends Packet, R> void remove(GroupContext<SessionContext, P, R> groupContext, String clientIp, Integer clientPort, Throwable throwable,
			String remark)
	{
		ChannelContext<SessionContext, P, R> channelContext = groupContext.getClientNodes().find(clientIp, clientPort);
		remove(channelContext, throwable, remark);
	}
}
