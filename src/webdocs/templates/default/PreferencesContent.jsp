<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>

<wiki:TabbedSection defaultTab="${param.tab}">

  <wiki:Tab id="prefs" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "prefs.tab.prefs")%>" accesskey="p" >
     <wiki:Include page="PreferencesTab.jsp" />
  </wiki:Tab>

  <wiki:Permission permission="editProfile">
  <wiki:Tab id="profile" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "prefs.tab.profile")%>" accesskey="o" >
     <wiki:Include page="ProfileTab.jsp" />
  </wiki:Tab>
  </wiki:Permission>
  
  <wiki:Permission permission="createGroups"> <!-- FIXME check right permissions -->
  <wiki:Tab id="group" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "group.tab")%>" accesskey="g" >
    <wiki:Include page="GroupTab.jsp" />
  </wiki:Tab>
  </wiki:Permission>

</wiki:TabbedSection>