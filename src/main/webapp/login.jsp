<html>
<head>
<title>Login Page</title>
<style>
th,td {
	padding: 5;
}

th {
	text-align: left
}
</style>
</head>

<body onload="document.f.username.focus();">
	<h3>Login with Username and Password (Custom Page)</h3>

	<form name="f" action="/login.jsp" method="post">
		<input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
		<fieldset>
			<legend>Please Login</legend>
			<%
				if (request.getParameter("error") != null) {
			%>
			<div>
				Failed to login.
				<%
				if (session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION") != null) {
			%>
				Reason:
				<%=((Throwable) session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION")).getMessage()%>
				<%
					}
				%>
			</div>
			<%
				}
			%>

			<%
				if (request.getParameter("logout") != null) {
			%>
			<div>You have been logged out.</div>
			<%
				}
			%>
			<p>
				<label for="username">Username</label> <input type="text" id="username" name="username" />
			</p>
			<p>
				<label for="password">Password</label> <input type="password" id="password" name="password" />
			</p>
			<p>
				<label for="totpkey">Google Authenticator Code</label> <input type="text" id="totpkey" name="totpkey" />
			</p>
			<p>
				<label for="remember-me">Remember Me?</label> <input type="checkbox" id="remember-me" name="remember-me" />
			</p>
			<div>
				<button type="submit" class="btn">Log in</button>
			</div>
		</fieldset>
	</form>

	<p>
	<table>
		<thead>
			<tr>
				<th>Username</th>
				<th>Password</th>
				<th>Key</th>
			</tr>
		</thead>
		<tbody>
			<tr>
				<td>admin</td>
				<td>admin</td>
				<td>IB6EFEQKE7U2TQIB</td>
			</tr>
			<tr>
				<td>user</td>
				<td>user</td>
				<td>BPPGGZTFHRWDUA67</td>
			</tr>
		</tbody>
	</table>

</body>
</html>