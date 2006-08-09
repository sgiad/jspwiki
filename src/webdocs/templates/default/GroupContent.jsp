<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<%@ page import="java.security.Principal" %>
<%@ page import="java.util.*" %>
<%@ page import="com.ecyrd.jspwiki.WikiContext" %>
<%@ page import="com.ecyrd.jspwiki.auth.PrincipalComparator" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.Group" %>
<%@ page import="com.ecyrd.jspwiki.auth.authorize.GroupManager" %>
<%@ page import="org.apache.log4j.*" %>
<%@ page errorPage="/Error.jsp" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.DefaultResources"/>
<%! 
    Logger log = Logger.getLogger("JSPWiki"); 
%>

<%
  // Extract the group name and members
  String name = request.getParameter( "group" );
  Group group = (Group)pageContext.getAttribute( "Group",PageContext.REQUEST_SCOPE );
  Principal[] members = null;
  Date modified = null;
  Date created = null;
  String modifier = "";
  String creator = "";
  
  if ( group != null )
  {
    name = group.getName();
    members = group.members();
    Arrays.sort( members, new PrincipalComparator() );
    creator = group.getCreator();
    if ( group.getCreated() != null )
    {
      created = group.getCreated();
    }
    modifier = group.getModifier();
    if ( group.getLastModified() != null )
    {
      modified = group.getLastModified();
    }
  }
%>


<script language="javascript" type="text/javascript">
function confirmDelete()
{
  var reallydelete = confirm("<fmt:message key="group.areyousure"><fmt:param><%=group%></fmt:param></fmt:message>");

  return reallydelete;
}
</script>

<h3>Group <%=name%></h3>

<%
  if ( group == null )
  {
    WikiContext c = WikiContext.findContext( pageContext );
    
    if ( c.getWikiSession().getMessages( GroupManager.MESSAGES_KEY ).length == 0 )
    {
%>
    <fmt:message key="group.doesnotexist"/>
    <wiki:Permission permission="createGroups">
      <fmt:message key="group.createsuggestion">
        <fmt:param><wiki:Link jsp="NewGroup.jsp">
                      <wiki:Param name="group" value="<%=name%>" />
                      <wiki:Param name="group" value="<%=name%>" />
                      <fmt:message key="group.createit"/>
                   </wiki:Link>
        </fmt:param>
      </fmt:message>
    </wiki:Permission>
<%
    }
    else
    {
%>
       <wiki:Messages div="error" topic="<%=GroupManager.MESSAGES_KEY%>" prefix="<%=LocaleSupport.getLocalizedMessage(pageContext,"group.errorprefix") %> "/>
<%
    }
  }
  else
  {
%>
    <div class="formcontainer">
      <div class="instructions">
        <fmt:message key="group.groupintro">
          <fmt:param><em><%=name%></em></fmt:param>
        </fmt:message>
      </div>
    
      <!-- Members -->
      <div class="block">
        <label><fmt:message key="group.members"/></label>
        <div class="readonly"><%
            for ( int i = 0; i < members.length; i++ )
            {
              out.println( members[i].getName().trim() );
              if ( i < ( members.length - 1 ) )
              {
                out.println( "<br/>" );
              }
            }
          %></div>
        <div class="description">
          <fmt:message key="group.membership"/>
        </div>
      </div>
      
      <div class="instructions">
        <fmt:message key="group.modifier">
           <fmt:param><%=modifier%></fmt:param>
           <fmt:param><%=modified%></fmt:param>
        </fmt:message>
        <br />
        <fmt:message key="group.creator">
           <fmt:param><%=creator%></fmt:param>
           <fmt:param><%=created%></fmt:param>
        </fmt:message> 
      </div>
    </div>
<%
  }
%>

