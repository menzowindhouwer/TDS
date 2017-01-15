<?xml version="1.0"?>
<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

	<xsl:output method="xml" encoding="utf-8"/>

	<xsl:param name="keystr"/>
	<xsl:param name="ignorestr"/>

	<xsl:variable name="doc" select="/"/>

	<xsl:template match="/">
		<xsl:variable name="keys" select="tokenize($keystr,'\s+')"/>
		<xsl:variable name="k" select="distinct-values(for $tuple in //tuple return (string-join(for $key in $keys return (($tuple/field[@name=$key])[1]),'')))"/>
		<xsl:variable name="ignore" select="tokenize($ignorestr,'\s+')"/>
		<answer>
			<xsl:for-each select="$k">
				<xsl:variable name="row" select="position()"/>
				<xsl:variable name="ck" select="current()"/>
				<tuple row="{$row}" key="{$ck}">
					<xsl:for-each-group select="$doc//tuple[string-join(for $key in $keys return ((field[@name=$key])[1]),'')=$ck]/field" group-by="@name">
						<xsl:sort select="current-grouping-key()"/>
						<xsl:if test="empty(index-of($ignore,current-grouping-key()))">
							<xsl:if test="count(current-group())&gt;1 and empty(index-of($keys,current-grouping-key())) and count(distinct-values(current-group()))&gt;1">
								<xsl:message>!WARNING:row[<xsl:value-of select="$row"/>] key[<xsl:value-of select="$ck"/>] field[<xsl:value-of select="current-grouping-key()"/>] found several times with different values[<xsl:for-each select="distinct-values(current-group())"><xsl:value-of select="."/><xsl:if test="position()!=last()">,</xsl:if></xsl:for-each>] taking value[<xsl:value-of select="current-group()[1]"/>]</xsl:message>
							</xsl:if>
							<xsl:apply-templates mode="copy" select="current-group()[1]"/>
						</xsl:if>
					</xsl:for-each-group>
				</tuple>
			</xsl:for-each>
		</answer>
	</xsl:template>

	<xsl:template match="@*|node()" mode="copy">
		<xsl:copy>
			<xsl:apply-templates select="@*|node()" mode="copy"/>
		</xsl:copy>
	</xsl:template>

</xsl:stylesheet>
