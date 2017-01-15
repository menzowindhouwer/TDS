<?xml version="1.0"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
  xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
>

	<xsl:variable name="NL" select="system-property('line.separator')"/>

	<xsl:output method="text" encoding="utf-8"/>

	<xsl:template match="text()"/>

	<xsl:template match="svrl:failed-assert">
		<xsl:message>
			<xsl:text>context : </xsl:text>
			<xsl:value-of select="(preceding-sibling::svrl:fired-rule)[last()]/@context"/>
			<xsl:value-of select="$NL"/>
			<xsl:text>test    : </xsl:text>
			<xsl:value-of select="@test"/>
			<xsl:value-of select="$NL"/>
			<xsl:text>location: </xsl:text>
			<xsl:value-of select="@location"/>
			<xsl:value-of select="$NL"/>
			<xsl:choose>
				<xsl:when test="(preceding-sibling::svrl:fired-rule)[last()]/@role='warning'">
					<xsl:text>WARNING : </xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>ERROR   : </xsl:text>
				</xsl:otherwise>
			</xsl:choose>
			<xsl:value-of select="svrl:text"/>
			<xsl:value-of select="$NL"/>
			<xsl:value-of select="$NL"/>
		</xsl:message>
	</xsl:template>

	<xsl:template match="/">
		<xsl:apply-templates/>
		<xsl:choose>
			<xsl:when test="exists(//svrl:failed-assert[(preceding-sibling::svrl:fired-rule)[last()][empty(@role) or @role!='warning']])">
				<xsl:message terminate="yes">
					<xsl:text>RESULT  : The document is invalid!</xsl:text>
					<xsl:value-of select="$NL"/>
				</xsl:message>
			</xsl:when>
			<xsl:when test="exists(//svrl:failed-assert[(preceding-sibling::svrl:fired-rule)[last()]/@role='warning'])">
				<xsl:message>
					<xsl:text>RESULT  : The document is valid, but there are warnings!</xsl:text>
					<xsl:value-of select="$NL"/>
				</xsl:message>
			</xsl:when>
			<xsl:otherwise>
				<xsl:message>
					<xsl:text>RESULT  : The document is valid.</xsl:text>
					<xsl:value-of select="$NL"/>
				</xsl:message>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

</xsl:stylesheet>
