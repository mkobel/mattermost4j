/*
 * @(#) net.bis5.mattermost.client4.MattermostApiTest
 * Copyright (c) 2017 Maruyama Takayuki
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * 	Unless required by applicable law or agreed to in writing, software
 * 	distributed under the License is distributed on an "AS IS" BASIS,
 * 	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 	See the License for the specific language governing permissions and
 * 	limitations under the License.
 */
package net.bis5.mattermost.client4;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.number.OrderingComparison.greaterThan;
import static org.hamcrest.text.IsEmptyString.isEmptyOrNullString;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import net.bis5.mattermost.model.Channel;
import net.bis5.mattermost.model.ChannelList;
import net.bis5.mattermost.model.ChannelMember;
import net.bis5.mattermost.model.ChannelMembers;
import net.bis5.mattermost.model.ChannelPatch;
import net.bis5.mattermost.model.ChannelStats;
import net.bis5.mattermost.model.ChannelType;
import net.bis5.mattermost.model.ChannelUnread;
import net.bis5.mattermost.model.ChannelView;
import net.bis5.mattermost.model.Post;
import net.bis5.mattermost.model.PostList;
import net.bis5.mattermost.model.Role;
import net.bis5.mattermost.model.Team;
import net.bis5.mattermost.model.User;

/**
 * Mattermost API call test
 * 
 * @author Maruyama Takayuki
 */
public class MattermostApiTest {

	private static final String APPLICATION = getApplicationUrl();
	private MattermostClient client;
	private static TestHelper th;

	private static String getApplicationUrl() {
		String url = System.getenv("MATTERMOST_URL");
		return url != null ? url : "http://localhost:8065";
	}

	@BeforeClass
	public static void initHelper() throws InterruptedException, ExecutionException {
		th = new TestHelper(new MattermostClient(APPLICATION)).setup();
	}

	@Before
	public void setup() throws InterruptedException, ExecutionException {
		client = new MattermostClient(APPLICATION);
		th.changeClient(client).initBasic();
	}

	@After
	public void tearDown() throws InterruptedException, ExecutionException {
		try {
			th.logout();
		} catch (InterruptedException | ExecutionException ex) {
			// avoid errors
		}
	}

	private <T> ApiResponse<T> checkNoError(ApiResponse<T> response) {
		return th.checkNoError(response);
	}

	private <T> ApiResponse<T> checkStatus(ApiResponse<T> response, Status status) {
		Response rawResponse = response.getRawResponse();

		assertThat(rawResponse.getStatus(), is(status.getStatusCode()));

		return response;
	}

	public static class HasError<T extends ApiResponse<U>, U> extends BaseMatcher<T> {

		private ApiResponse<U> actual;

		/**
		 * @see org.hamcrest.Matcher#matches(java.lang.Object)
		 */
		@SuppressWarnings("unchecked")
		@Override
		public boolean matches(Object actual) {
			if (actual instanceof ApiResponse) {
				this.actual = (ApiResponse<U>) actual;
				return this.actual.hasError();
			}
			return false;
		}

		/**
		 * @see org.hamcrest.SelfDescribing#describeTo(org.hamcrest.Description)
		 */
		@Override
		public void describeTo(Description description) {
			description.appendText("Should have failed");
		}

		public static <U> Matcher<? extends ApiResponse<U>> hasError() {
			return new HasError<>();
		}

	}

	// Channels

	@Test
	public void testChannels_CreateChannel_Open_Required() throws InterruptedException, ExecutionException {
		Channel channel = new Channel("DisplayName", "name", ChannelType.Open, th.basicTeam().getId());
		testChannels_CreateChannel_Success(channel);
	}

	@Test
	public void testChannels_CreateChannel_Open_All() throws InterruptedException, ExecutionException {
		Channel channel = new Channel("DisplayName", "name", ChannelType.Open, th.basicTeam().getId());
		channel.setPurpose("purpose");
		channel.setHeader("header");
		testChannels_CreateChannel_Success(channel);
	}

	@Test
	public void testChannels_CreateChannel_Private_Required() throws InterruptedException, ExecutionException {
		Channel channel = new Channel("DisplayName", "name", ChannelType.Private, th.basicTeam().getId());
		testChannels_CreateChannel_Success(channel);
	}

	@Test
	public void testChannels_CreateChannel_Private_All() throws InterruptedException, ExecutionException {
		Channel channel = new Channel("DisplayName", "name", ChannelType.Private, th.basicTeam().getId());
		channel.setPurpose("purpose");
		channel.setHeader("header");
		testChannels_CreateChannel_Success(channel);
	}

	@Test
	public void testChannels_CreateChannel_Fail_Direct() {
		Channel channel = new Channel("DisplayName", "name", ChannelType.Direct, th.basicTeam().getId());

		client.createChannel(channel)
				.thenApply(r -> checkStatus(r, Status.BAD_REQUEST));
	}

	private void testChannels_CreateChannel_Success(Channel channel) throws InterruptedException, ExecutionException {
		String teamId = channel.getTeamId();
		String name = channel.getName();
		String displayName = channel.getDisplayName();
		ChannelType type = channel.getType();
		String purpose = channel.getPurpose();
		String header = channel.getHeader();

		channel = client.createChannel(channel)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(channel.getTeamId(), is(teamId));
		assertThat(channel.getName(), is(name));
		assertThat(channel.getDisplayName(), is(displayName));
		assertThat(channel.getType(), is(type));
		// optional properties
		assertThat(channel.getPurpose(), purpose == null ? isEmptyOrNullString() : is(purpose));
		assertThat(channel.getHeader(), header == null ? isEmptyOrNullString() : is(header));
	}

	@Test
	public void testChannels_CreateDirectChannel() throws InterruptedException, ExecutionException {
		User user1 = th.basicUser();
		User user2 = th.basicUser2();

		Channel channel = client.createDirectChannel(user1.getId(), user2.getId())
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(channel, is(notNullValue()));
	}

	@Test
	public void testChannels_CreateDirectChannel_OneUser() throws InterruptedException, ExecutionException {
		client.createDirectChannel(th.basicUser().getId(), null)
				.thenApply(r -> this.checkStatus(r, Status.BAD_REQUEST))
				.toCompletableFuture().get();
	}

	@Test
	public void testChannels_CreateGroupChannel() throws InterruptedException, ExecutionException {
		User user1 = th.basicUser();
		User user2 = th.basicUser2();
		User user3 = th.createUser();
		th.loginSystemAdmin().linkUserToTeam(user3, th.basicTeam()).loginBasic();

		client.createGroupChannel(user1.getId(), user2.getId(), user3.getId())
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();
	}

	@Test
	public void testChannels_CreateGroupChannel_Fail_TwoUsers() throws InterruptedException, ExecutionException {
		User user1 = th.basicUser();
		User user2 = th.basicUser2();

		client.createGroupChannel(user1.getId(), user2.getId())
				.thenApply(r -> checkStatus(r, Status.BAD_REQUEST))
				.toCompletableFuture().get();
	}

	@Test
	public void testChannels_ChannelListByTeamId() throws InterruptedException, ExecutionException {
		Team theTeam = th.loginSystemAdmin().createTeam();
		User theUser = th.createUser();
		th.linkUserToTeam(theUser, theTeam);
		Channel channel1 = new Channel("displayname1", "name1", ChannelType.Open, theTeam.getId());
		Channel channel2 = new Channel("displayname2", "name2", ChannelType.Open, theTeam.getId());
		channel1 = client.createChannel(channel1).thenApply(ApiResponse::readEntity).toCompletableFuture().get();
		channel2 = client.createChannel(channel2).thenApply(ApiResponse::readEntity).toCompletableFuture().get();
		th.loginAs(theUser);

		ChannelList channels = client.getPublicChannelsByIdsForTeam(theTeam.getId(), channel1.getId(), channel2.getId())
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		List<String> ids = channels.stream()
				.map(Channel::getId)
				.collect(Collectors.toList());
		assertThat(ids.size(), is(2));
		assertThat(ids, containsInAnyOrder(channel1.getId(), channel2.getId()));
	}

	@Test
	public void testChannels_GetAChannel() throws InterruptedException, ExecutionException {
		String channelId = th.basicChannel().getId();

		Channel channel = client.getChannel(channelId, null)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(channel.getId(), is(channelId));
	}

	@Test
	public void testChannels_UpdateChannel() throws InterruptedException, ExecutionException {
		String channelId = th.basicChannel().getId();
		String newName = "new-channel-name";
		String newDispName = "New Display Name";
		String newPurpose = "New Purpose";
		String newHeader = "New Header";
		Channel newChannel = new Channel();
		newChannel.setId(channelId);
		newChannel.setName(newName);
		newChannel.setDisplayName(newDispName);
		newChannel.setPurpose(newPurpose);
		newChannel.setHeader(newHeader);

		newChannel = client.updateChannel(newChannel)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(newChannel.getName(), is(newName));
		assertThat(newChannel.getDisplayName(), is(newDispName));
		assertThat(newChannel.getPurpose(), is(newPurpose));
		assertThat(newChannel.getHeader(), is(newHeader));
	}

	@Test
	public void testChannels_UpdateChannel_ChannelNotFound() throws InterruptedException, ExecutionException {
		String channelId = th.newId();
		String newName = "new-channel-name";
		String newDispName = "New Display Name";
		String newPurpose = "New Purpose";
		String newHeader = "New Header";
		Channel newChannel = new Channel();
		newChannel.setId(channelId);
		newChannel.setName(newName);
		newChannel.setDisplayName(newDispName);
		newChannel.setPurpose(newPurpose);
		newChannel.setHeader(newHeader);

		client.updateChannel(newChannel)
				.thenApply(r -> checkStatus(r, Status.NOT_FOUND))
				.toCompletableFuture().get();
	}

	@Test
	public void testChannels_UpdateChannel_ChangeType() throws InterruptedException, ExecutionException {
		String channelId = th.basicChannel().getId();
		assertThat(th.basicChannel().getType(), is(ChannelType.Open));
		ChannelType newType = ChannelType.Private;
		Channel newChannel = new Channel();
		newChannel.setId(channelId);
		newChannel.setType(newType);

		newChannel = client.updateChannel(newChannel)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(newChannel.getType(), is(newType));
	}

	@Test
	public void testChannels_DeleteChannel() throws InterruptedException, ExecutionException {
		String channelId = th.basicChannel().getId();

		boolean deleteResult = client.deleteChannel(channelId)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.thenApply(Boolean::booleanValue)
				.toCompletableFuture().get();

		assertThat(deleteResult, is(true));
		client.getChannel(channelId, null)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.thenAccept(ch -> assertThat(ch.getDeleteAt(), is(greaterThan(0l))))
				.toCompletableFuture().get();
	}

	@Test
	public void testChannels_PatchChannel() throws InterruptedException, ExecutionException {
		String channelId = th.basicChannel().getId();
		String newDispName = "new Display name";
		ChannelPatch patch = new ChannelPatch();
		patch.setDisplayName(newDispName);

		Channel newChannel = client.patchChannel(channelId, patch)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(newChannel.getDisplayName(), is(newDispName));
	}

	@Test
	public void testChannels_PatchChannel_ChannelNotFound() throws InterruptedException, ExecutionException {
		String channelId = th.newId();
		String newDispName = "new Display name";
		ChannelPatch patch = new ChannelPatch();
		patch.setDisplayName(newDispName);

		client.patchChannel(channelId, patch)
				.thenApply(r -> checkStatus(r, Status.NOT_FOUND))
				.toCompletableFuture().get();
	}

	@Test
	@Ignore
	public void testChannels_RestoreChannel() {
		// TODO since 3.10
	}

	@Test
	public void testChannels_GetChannelStatistics() throws InterruptedException, ExecutionException {
		String channelId = th.basicChannel().getId();

		ChannelStats stats = client.getChannelStats(channelId, null)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(stats.getChannelId(), is(channelId));
	}

	@Test
	public void testChannels_GetChannelPinnedPosts() throws InterruptedException, ExecutionException {
		String channelId = th.basicChannel().getId();
		Post pinned = th.createPinnedPost(channelId);

		PostList posts = client.getPinnedPosts(channelId, null)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(posts.size(), is(1));
		assertThat(posts.getPosts().get(pinned.getId()), is(notNullValue()));
	}

	@Test
	public void testChannels_GetChannelByName() throws InterruptedException, ExecutionException {
		String channelName = th.basicChannel().getName();

		Channel channel = client.getChannelByName(channelName, th.basicTeam().getId(), null)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(channel.getId(), is(th.basicChannel().getId()));
	}

	@Test
	public void testChannels_GetChannelByName_ChannelNotFound() throws InterruptedException, ExecutionException {
		String channelName = "fake-channel-name";

		client.getChannelByName(channelName, th.basicTeam().getId(), null)
				.thenApply(r -> checkStatus(r, Status.NOT_FOUND))
				.toCompletableFuture().get();
	}

	@Test
	public void testChannels_GetChannelByNameAndTeamName() throws InterruptedException, ExecutionException {
		String channelName = th.basicChannel().getName();
		String teamName = th.basicTeam().getName();

		Channel channel = client.getChannelByNameForTeamName(channelName, teamName, null)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(channel.getId(), is(th.basicChannel().getId()));
	}

	@Test
	public void testChannels_GetChannelByNameAndTeamName_ChannelNotFound()
			throws InterruptedException, ExecutionException {
		String channelName = "fake-channel-name";
		String teamName = th.basicTeam().getName();

		client.getChannelByNameForTeamName(channelName, teamName, null)
				.thenApply(r -> checkStatus(r, Status.NOT_FOUND))
				.toCompletableFuture().get();
	}

	@Test
	public void testChannels_GetChannelByNameAndTeamName_TeamNotFound()
			throws InterruptedException, ExecutionException {
		String channelName = "fake-channel-name";
		String teamName = "fake-team-name";

		client.getChannelByNameForTeamName(channelName, teamName, null)
				.thenApply(r -> checkStatus(r, Status.NOT_FOUND))
				.toCompletableFuture().get();
	}

	@Test
	public void testChannels_GetChannelMembers() throws InterruptedException, ExecutionException {
		User user1 = th.createUser();
		User user2 = th.createUser();
		th.loginSystemAdmin().linkUserToTeam(user1, th.basicTeam()).linkUserToTeam(user2, th.basicTeam())
				.loginAs(user1);
		Channel channel = th.createPublicChannel();
		CompletableFuture.allOf(client.addChannelMember(channel.getId(), user1.getId()).toCompletableFuture(),
				client.addChannelMember(channel.getId(), user2.getId()).toCompletableFuture()).get();

		ChannelMembers members = client.getChannelMembers(channel.getId(), 0, 60, null)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(members.size(), is(2));
		assertThat(members.stream().map(m -> m.getUserId()).collect(Collectors.toSet()),
				containsInAnyOrder(user1.getId(), user2.getId()));
	}

	@Test
	public void testChannels_AddUser() throws InterruptedException, ExecutionException {
		Channel channel = th.basicChannel();
		User user = th.createUser();
		th.loginSystemAdmin().linkUserToTeam(user, th.basicTeam()).loginBasic();

		ChannelMember member = client.addChannelMember(channel.getId(), user.getId())
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(member.getChannelId(), is(channel.getId()));
		assertThat(member.getUserId(), is(user.getId()));
	}

	@Test
	public void testChannels_GetChannelMembersByIds() throws InterruptedException, ExecutionException {
		Channel channel = th.createPublicChannel();
		User user1 = th.createUser();
		User user2 = th.createUser();
		th.loginSystemAdmin().linkUserToTeam(user1, th.basicTeam()).linkUserToTeam(user2, th.basicTeam()).loginBasic();
		CompletableFuture.allOf(client.addChannelMember(channel.getId(), user1.getId()).toCompletableFuture(),
				client.addChannelMember(channel.getId(), user2.getId()).toCompletableFuture()).get();

		ChannelMembers members = client.getChannelMembersByIds(channel.getId(), user1.getId(), user2.getId())
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(members.size(), is(2));
		assertThat(members.stream().map(m -> m.getUserId()).collect(Collectors.toSet()),
				containsInAnyOrder(user1.getId(), user2.getId()));
	}

	@Test
	public void testChannels_GetChannelMember() throws InterruptedException, ExecutionException {
		Channel channel = th.basicChannel();
		User user = th.basicUser();

		ChannelMember member = client.getChannelMember(channel.getId(), user.getId(), null)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(member.getChannelId(), is(channel.getId()));
		assertThat(member.getUserId(), is(user.getId()));
	}

	@Test
	public void testChannels_RemoveUserFromChannel() throws InterruptedException, ExecutionException {
		Channel channel = th.basicChannel();
		User user = th.basicUser2();

		// logged-in as basicUser

		boolean result = client.removeUserFromChannel(channel.getId(), user.getId())
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.thenApply(Boolean::booleanValue)
				.toCompletableFuture().get();

		assertThat(result, is(true));
	}

	@Test
	public void testChannels_UpdateChannelRoles() throws InterruptedException, ExecutionException {
		User channelAdmin = th.basicUser();
		User channelUser = th.basicUser2();
		Channel channel = th.loginAs(channelAdmin).createPublicChannel();
		client.addChannelMember(channel.getId(), channelUser.getId()).toCompletableFuture().get();

		boolean result = client
				.updateChannelRoles(channel.getId(), channelUser.getId(), Role.ROLE_CHANNEL_ADMIN,
						Role.ROLE_CHANNEL_USER)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.thenApply(Boolean::booleanValue)
				.toCompletableFuture().get();

		assertThat(result, is(true));
	}

	@Test
	@Ignore
	public void testChannels_UpdateChannelNotifications() {
		// TODO props定数を作る
	}

	@Test
	public void testChannels_ViewChannel() throws InterruptedException, ExecutionException {
		User user = th.basicUser();
		Channel channel = th.basicChannel2();
		ChannelView view = new ChannelView(channel.getId());

		boolean result = client.viewChannel(user.getId(), view)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.thenApply(Boolean::booleanValue)
				.toCompletableFuture().get();

		assertThat(result, is(true));
	}

	@Test
	public void testChannels_GetChannelMembersForUser() throws InterruptedException, ExecutionException {
		User user = th.createUser();
		th.loginSystemAdmin().linkUserToTeam(user, th.basicTeam()).loginAs(user);
		Channel channel1 = th.createPublicChannel();
		Channel channel2 = th.createPublicChannel();
		CompletableFuture.allOf(client.addChannelMember(channel1.getId(), user.getId()).toCompletableFuture(),
				client.addChannelMember(channel2.getId(), user.getId()).toCompletableFuture()).get();

		ChannelMembers members = client.getChannelMembersForUser(user.getId(), th.basicTeam().getId(), null)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(members.stream().map(m -> m.getChannelId()).collect(Collectors.toSet()),
				hasItems(channel1.getId(), channel2.getId()));
	}

	@Test
	public void testChannels_GetChannelsForUser() throws InterruptedException, ExecutionException {
		User user = th.createUser();
		th.loginSystemAdmin().linkUserToTeam(user, th.basicTeam()).loginAs(user);
		Channel channel1 = th.createPublicChannel();
		Channel channel2 = th.createPublicChannel();
		CompletableFuture.allOf(client.addChannelMember(channel1.getId(), user.getId()).toCompletableFuture(),
				client.addChannelMember(channel2.getId(), user.getId()).toCompletableFuture()).get();

		ChannelList channels = client.getChannelsForTeamForUser(th.basicTeam().getId(), user.getId(), null)
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(channels.stream().map(c -> c.getId()).collect(Collectors.toSet()),
				hasItems(channel1.getId(), channel2.getId()));
	}

	@Test
	public void testChannels_GetUnreadMessages() throws InterruptedException, ExecutionException {
		User user = th.basicUser();
		Channel channel = th.basicChannel();

		ChannelUnread unread = client.getChannelUnread(channel.getId(), user.getId())
				.thenApply(this::checkNoError)
				.thenApply(ApiResponse::readEntity)
				.toCompletableFuture().get();

		assertThat(unread.getChannelId(), is(channel.getId()));
	}

}
