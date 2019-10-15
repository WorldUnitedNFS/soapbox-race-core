package com.soapboxrace.core.api;

import com.soapboxrace.core.api.util.Secured;
import com.soapboxrace.core.bo.DriverPersonaBO;
import com.soapboxrace.core.bo.PresenceBO;
import com.soapboxrace.core.bo.TokenSessionBO;
import com.soapboxrace.core.bo.UserBO;
import com.soapboxrace.core.engine.EngineException;
import com.soapboxrace.core.engine.EngineExceptionCode;
import com.soapboxrace.core.jpa.PersonaEntity;
import com.soapboxrace.jaxb.http.*;
import com.soapboxrace.jaxb.util.UnmarshalXML;

import javax.ejb.EJB;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import java.io.InputStream;
import java.util.regex.Pattern;

@Path("/DriverPersona")
public class DriverPersona {
    private final Pattern NAME_PATTERN = Pattern.compile("^[A-Z0-9]{3,15}$");

    @EJB
    private DriverPersonaBO driverPersonaBO;

    @EJB
    private UserBO userBo;

    @EJB
    private TokenSessionBO tokenSessionBo;

    @EJB
    private PresenceBO presenceBO;

    @GET
    @Secured
    @Path("/GetExpLevelPointsMap")
    @Produces(MediaType.APPLICATION_XML)
    public ArrayOfInt getExpLevelPointsMap() {
        return driverPersonaBO.getExpLevelPointsMap();
    }

    @GET
    @Secured
    @Path("/GetPersonaInfo")
    @Produces(MediaType.APPLICATION_XML)
    public ProfileData getPersonaInfo(@QueryParam("personaId") Long personaId) {
        return driverPersonaBO.getPersonaInfo(personaId);
    }

    @POST
    @Secured
    @Path("/ReserveName")
    @Produces(MediaType.APPLICATION_XML)
    public ArrayOfString reserveName(@QueryParam("name") String name) {
        return driverPersonaBO.reserveName(name);
    }

    @POST
    @Secured
    @Path("/UnreserveName")
    @Produces(MediaType.APPLICATION_XML)
    public String UnreserveName(@QueryParam("name") String name) {
        return "";
    }

    @POST
    @Secured
    @Path("/CreatePersona")
    @Produces(MediaType.APPLICATION_XML)
    public ProfileData createPersona(@HeaderParam("userId") Long userId,
                                     @HeaderParam("securityToken") String securityToken,
                                     @QueryParam("name") String name,
                                     @QueryParam("iconIndex") int iconIndex, @QueryParam("clan") String clan,
                                     @QueryParam("clanIcon") String clanIcon) {
        if (!NAME_PATTERN.matcher(name).matches()) {
            throw new EngineException(EngineExceptionCode.DisplayNameNotAllowed);
        }

        ArrayOfString nameReserveResult = driverPersonaBO.reserveName(name);

        if (!nameReserveResult.getString().isEmpty()) {
            throw new EngineException(EngineExceptionCode.DisplayNameDuplicate);
        }

        PersonaEntity personaEntity = new PersonaEntity();
        personaEntity.setName(name);
        personaEntity.setIconIndex(iconIndex);
        ProfileData persona = driverPersonaBO.createPersona(userId, personaEntity);

        if (persona == null) {
            throw new EngineException(EngineExceptionCode.MaximumNumberOfPersonasForUserReached);
        }

        long personaId = persona.getPersonaId();
        userBo.createXmppUser(personaId, securityToken.substring(0, 16));
        return persona;
    }

    @POST
    @Secured
    @Path("/DeletePersona")
    @Produces(MediaType.APPLICATION_XML)
    public String deletePersona(@QueryParam("personaId") Long personaId,
                                @HeaderParam("securityToken") String securityToken) {
        tokenSessionBo.verifyPersonaOwnership(securityToken, personaId);
        driverPersonaBO.deletePersona(personaId);
        return "<long>0</long>";
    }

    @POST
    @Path("/GetPersonaBaseFromList")
    @Produces(MediaType.APPLICATION_XML)
    public ArrayOfPersonaBase getPersonaBaseFromList(InputStream is) {
        PersonaIdArray personaIdArray = UnmarshalXML.unMarshal(is, PersonaIdArray.class);
        ArrayOfLong personaIds = personaIdArray.getPersonaIds();
        return driverPersonaBO.getPersonaBaseFromList(personaIds.getLong());
    }

    @POST
    @Secured
    @Path("/UpdatePersonaPresence")
    @Produces(MediaType.APPLICATION_XML)
    public String updatePersonaPresence(@HeaderParam("securityToken") String securityToken,
                                        @QueryParam("presence") Long presence) {
        if (tokenSessionBo.getActivePersonaId(securityToken) == 0L)
            throw new EngineException(EngineExceptionCode.FailedSessionSecurityPolicy);
        presenceBO.updatePresence(tokenSessionBo.getActivePersonaId(securityToken), presence);

        return "";
    }

    @GET
    @Secured
    @Path("/GetPersonaPresenceByName")
    @Produces(MediaType.APPLICATION_XML)
    public PersonaPresence getPersonaPresenceByName(@QueryParam("displayName") String displayName) {
        PersonaPresence personaPresenceByName = driverPersonaBO.getPersonaPresenceByName(displayName);
        if (personaPresenceByName.getPersonaId() == 0) {
            throw new EngineException(EngineExceptionCode.PersonaNotFound);
        }
        return personaPresenceByName;
    }

    @POST
    @Secured
    @Path("/UpdateStatusMessage")
    @Produces(MediaType.APPLICATION_XML)
    public PersonaMotto updateStatusMessage(InputStream statusXml, @HeaderParam("securityToken") String securityToken
            , @Context Request request) {
        PersonaMotto personaMotto = UnmarshalXML.unMarshal(statusXml, PersonaMotto.class);
        tokenSessionBo.verifyPersonaOwnership(securityToken, personaMotto.getPersonaId());

        driverPersonaBO.updateStatusMessage(personaMotto.getMessage(), personaMotto.getPersonaId());
        return personaMotto;
    }
}