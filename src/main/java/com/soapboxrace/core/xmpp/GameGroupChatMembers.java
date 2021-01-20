package com.soapboxrace.core.xmpp;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

@XmlRootElement(name = "members")
public class GameGroupChatMembers {
    List<String> members;

    public GameGroupChatMembers() {
    }

    public GameGroupChatMembers(List<String> members) {
        this.members = members;
    }

    @XmlElement(name = "member")
    public List<String> getMembers() {
        return members;
    }

    public void setMembers(List<String> members) {
        this.members = members;
    }
}
