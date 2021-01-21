/*
 * This file is part of the Soapbox Race World core source code.
 * If you use any of this code for third-party purposes, please provide attribution.
 * Copyright (c) 2020.
 */

package com.soapboxrace.core.xmpp;

import com.soapboxrace.core.bo.ParameterBO;
import com.soapboxrace.core.dao.ChatRoomDAO;
import com.soapboxrace.core.jpa.ChatRoomEntity;
import org.igniterealtime.restclient.entity.MUCRoomEntity;
import org.igniterealtime.restclient.entity.UserEntity;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Startup
@Singleton
public class OpenFireRestApiCli {
    private String openFireToken;
    private String openFireAddress;
    private String xmppIp;
    private boolean restApiEnabled = false;

    @EJB
    private ParameterBO parameterBO;

    @EJB
    private ChatRoomDAO chatRoomDAO;

    @PostConstruct
    public void init() {
        openFireToken = parameterBO.getStrParam("OPENFIRE_TOKEN");
        openFireAddress = parameterBO.getStrParam("OPENFIRE_ADDRESS");
        xmppIp = parameterBO.getStrParam("XMPP_IP");
        if (openFireToken != null && openFireAddress != null) {
            restApiEnabled = true;
        }
        createUpdatePersona("sbrw.engine.engine", openFireToken);

        for (ChatRoomEntity chatRoomEntity : chatRoomDAO.findAll()) {
            for (int i = 1; i <= chatRoomEntity.getAmount(); i++) {
                createGeneralChatRoom(chatRoomEntity.getShortName(), i);
            }
        }
    }

    private Builder getBuilder(String path) {
        return getBuilder(path, null);
    }

    private Builder getBuilder(String path, Map<String, Object> query) {
        Client client = ClientBuilder.newClient();
        WebTarget target = client.target(openFireAddress).path(path);

        if (query != null) {
            for (Map.Entry<String, Object> queryEntry : query.entrySet()) {
                target = target.queryParam(queryEntry.getKey(), queryEntry.getValue());
            }
        }

        return target.request(MediaType.APPLICATION_XML).header("Authorization", openFireToken);
    }

    public void createUpdatePersona(String user, String password) {
        if (!restApiEnabled) {
            return;
        }
        Builder builder = getBuilder("users/" + user);
        Response response = builder.get();
        if (response.getStatus() == 200) {
            response.close();
            UserEntity userEntity = builder.get(UserEntity.class);
            userEntity.setPassword(password);
            builder = getBuilder("users/" + user);
            builder.put(Entity.entity(userEntity, MediaType.APPLICATION_XML));
        } else {
            response.close();
            builder = getBuilder("users");
            UserEntity userEntity = new UserEntity(user, null, null, password);
            builder.post(Entity.entity(userEntity, MediaType.APPLICATION_XML));
        }
        response.close();
    }

    public void createUpdatePersona(Long personaId, String password) {
        String user = "sbrw." + personaId.toString();
        createUpdatePersona(user, password);
    }

    public int getTotalOnlineUsers() {
        if (!restApiEnabled) {
            return 0;
        }
        Builder builder = getBuilder("system/statistics/sessions");
        SessionsCount sessionsCount = builder.get(SessionsCount.class);
        int clusterSessions = sessionsCount.getClusterSessions();
        if (clusterSessions > 1) {
            return clusterSessions - 1;
        }
        return 0;
    }

    public List<Long> getAllPersonaByGroup(Long personaId) {
        if (!restApiEnabled) {
            return new ArrayList<>();
        }
        Builder builder = getBuilder("chatrooms/getPersonaGroupMembers", Map.of("userName", "sbrw." + personaId));
        GameGroupChatMembers gameGroupChatMembers = builder.get(GameGroupChatMembers.class);

        if (gameGroupChatMembers.getMembers() == null) {
            return new ArrayList<>();
        }

        return gameGroupChatMembers.getMembers()
                .stream()
                .map(s -> Long.parseLong(s.split("\\.")[1]))
                .collect(Collectors.toList());
    }

    public void sendChatAnnouncement(String message) {
        Builder builder = getBuilder("messages/game");
        builder.post(Entity.entity(message, MediaType.TEXT_PLAIN_TYPE));
    }

    private void createGeneralChatRoom(String language, Integer number) {
        String name = "channel." + language + "__" + number;
        Builder builder = getBuilder("chatrooms");
        MUCRoomEntity mucRoomEntity = new MUCRoomEntity();
        mucRoomEntity.setRoomName(name);
        mucRoomEntity.setNaturalName(name);
        mucRoomEntity.setDescription(name);
        mucRoomEntity.setMaxUsers(0);
        mucRoomEntity.setPersistent(true);
        mucRoomEntity.setBroadcastPresenceRoles(Arrays.asList("moderator", "participant", "visitor"));
        mucRoomEntity.setLogEnabled(true);

        builder.post(Entity.entity(mucRoomEntity, MediaType.APPLICATION_XML));
    }
}