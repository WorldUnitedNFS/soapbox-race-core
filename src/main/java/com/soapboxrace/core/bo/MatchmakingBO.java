/*
 * This file is part of the Soapbox Race World core source code.
 * If you use any of this code for third-party purposes, please provide attribution.
 * Copyright (c) 2020.
 */

package com.soapboxrace.core.bo;

import com.soapboxrace.core.dao.PersonaDAO;
import com.soapboxrace.core.events.PersonaPresenceUpdated;
import com.soapboxrace.core.jpa.EventEntity;
import com.soapboxrace.core.jpa.LobbyEntity;
import com.soapboxrace.core.jpa.PersonaEntity;

import javax.ejb.*;
import javax.enterprise.event.Observes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsible for managing the multiplayer matchmaking system.
 * This deals with 2 classes of events: restricted and open.
 * When asked for a persona for a given car class, the matchmaker
 * will check if that class is open or restricted. Open events will receive
 * players of any class, while restricted events will only receive players of
 * the required class.
 *
 * @author heyitsleo
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class MatchmakingBO {

    private final Map<Long, Integer> queuedPlayers = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> ignoredEvents = new ConcurrentHashMap<>();

    @EJB
    private PersonaDAO personaDAO;

    @EJB
    private LobbyMessagingBO lobbyMessagingBO;

    /**
     * Adds the given persona ID to the queue under the given car class.
     *
     * @param personaId The ID of the persona to add to the queue.
     * @param carClass  The class of the persona's current car.
     */
    public void addPlayerToQueue(Long personaId, Integer carClass) {
        queuedPlayers.put(personaId, carClass);
    }

    /**
     * Removes the given persona ID from the queue.
     *
     * @param personaId The ID of the persona to remove from the queue.
     */
    public void removePlayerFromQueue(Long personaId) {
        queuedPlayers.remove(personaId);
    }

    /**
     * Send chat notifications to players who qualify for a lobby
     *
     * @param lobbyEntity The new lobby
     */
    public void sendLobbyChatNotifications(LobbyEntity lobbyEntity, PersonaEntity creatorPersona) {
        EventEntity event = lobbyEntity.getEvent();

        for (Map.Entry<Long, Integer> queueEntry : this.queuedPlayers.entrySet()) {
            Integer queueClass = queueEntry.getValue();

            // Make sure the queued player can actually join with the car they queued with
            if (!queueClass.equals(event.getCarClassHash()) && event.getCarClassHash() != 607077938) {
                continue;
            }
            Long queuePersonaId = queueEntry.getKey();

            // Don't invite the player to their own lobby - this shouldn't ever happen, but you never know!
            if (queuePersonaId.equals(lobbyEntity.getPersonaId())) {
                continue;
            }

            // Don't give players events that they don't want
            if (isEventIgnored(queuePersonaId, event.getId())) {
                continue;
            }
            PersonaEntity queuePersona = personaDAO.find(queuePersonaId);
            int queuePersonaLevel = queuePersona.getLevel();

            // Don't give players events that they can't join
            if (queuePersonaLevel < event.getMinLevel() || queuePersonaLevel > event.getMaxLevel()) {
                // Add the event to the player's ignore-list so we can skip it next time
                ignoreEvent(queuePersonaId, event.getId());
                continue;
            }

            lobbyMessagingBO.sendLobbyChatNotification(lobbyEntity, creatorPersona, queuePersona);
        }
    }

    /**
     * Add the given event ID to the list of ignored events for the given persona ID.
     *
     * @param personaId the persona ID
     * @param eventId   the event ID
     */
    public void ignoreEvent(long personaId, long eventId) {
        getIgnoredEvents(personaId).add(eventId);
    }

    /**
     * Resets the list of ignored events for the given persona ID
     *
     * @param personaId the persona ID
     */
    public void resetIgnoredEvents(long personaId) {
        ignoredEvents.remove(personaId);
    }

    /**
     * Checks if the given event ID is in the list of ignored events for the given persona ID
     *
     * @param personaId the persona ID
     * @param eventId   the event ID
     * @return {@code true} if the given event ID is in the list of ignored events for the given persona ID
     */
    public boolean isEventIgnored(long personaId, long eventId) {
        return getIgnoredEvents(personaId).contains(eventId);
    }

    @Asynchronous
    @Lock(LockType.READ)
    public void handlePersonaPresenceUpdated(@Observes PersonaPresenceUpdated personaPresenceUpdated) {
        removePlayerFromQueue(personaPresenceUpdated.getPersonaId());
    }

    private List<Long> getIgnoredEvents(long personaId) {
        return ignoredEvents.computeIfAbsent(personaId, k -> new ArrayList<>());
    }
}
