<?xml version="1.0"?>
<xsl:stylesheet
  version="2.0"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:int="java:java.lang.Integer"
>
	<xsl:output method="html" encoding="utf-8"/>

	<xsl:variable name="sdoc" select="document('silipa93.xml')"/>
	<xsl:variable name="rdoc" select="document('robipa12a.xml')"/>

	<xsl:template name="map">
		<xsl:param name="code"/>
		<xsl:param name="map"/>
		<xsl:if test="count($code)&gt;1">
			<xsl:message>ERR: code[<xsl:value-of select="$code/@b"/>] appears <xsl:value-of select="count($code)"/> times in map[<xsl:value-of select="$map"/>]</xsl:message>
		</xsl:if>
		<td class="int">
			<xsl:if test="exists($code)">
				<xsl:for-each select="tokenize($code/@u,'\s+')">
					<xsl:value-of select="int:parseInt(current(),16)"/>
				</xsl:for-each>
			</xsl:if>
		</td>
		<td class="hex">
			<xsl:if test="exists($code)">
				<xsl:value-of select="$code/@u"/>
			</xsl:if>
		</td>
		<td class="{$map}">
			<xsl:if test="exists($code)">
				<xsl:text>&#160;&#160;</xsl:text>
				<xsl:for-each select="tokenize($code/@u,'\s+')">
					<xsl:text disable-output-escaping="yes">&amp;#x</xsl:text>
					<xsl:value-of select="current()"/>
					<xsl:text>;</xsl:text>
				</xsl:for-each>
				<xsl:text>&#160;&#160;</xsl:text>
			</xsl:if>
		</td>
	</xsl:template>

	<xsl:template name="main">
		<html>
			<head>
				<title>IPA mappings</title>
				<style type="text/css">
					tr.warn  { background-color: indianred; }
					td.int   { text-align: right; }
					td.hex   { text-align: right; }
					td.ipa   { font-family: "SILDoulosIPA"; text-align: center; }
					td.ipa93 { font-family: "SILDoulos IPA93"; text-align: center; }
					td.sil   { font-family: "Arial Unicode MS"; text-align: center; }
					td.rob   { font-family: "Arial Unicode MS"; text-align: center; }
				</style>
			</head>
			<body>
				<h1>IPA mappings</h1>
				<table>
					<thead>
						<tr>
							<th colspan="3">
								SIL IPA 1.2a
							</th>
							<th colspan="3">
								Rob IPA map
							</th>
							<th colspan="3">
								SIL IPA93
							</th>
							<th colspan="3">
								SIL IPA93 map
							</th>
						</tr>
						<tr>
							<th>
								code
							</th>
							<th>
								hex
							</th>
							<th>
								char
							</th>
							<th>
								code
							</th>
							<th>
								hex
							</th>
							<th>
								char
							</th>
							<th>
								code
							</th>
							<th>
								hex
							</th>
							<th>
								char
							</th>
							<th>
								code
							</th>
							<th>
								hex
							</th>
							<th>
								char
							</th>
						</tr>
					</thead>
					<tbody>
						<xsl:for-each select="32 to 255">
							<xsl:variable name="code" select="current()"/>
							<xsl:variable name="hex"  select="int:toHexString($code)"/>
							<xsl:variable name="sil"  select="$sdoc//a[upper-case(@b)=upper-case($hex)][empty(@bactxt)]"/>
							<xsl:variable name="rob"  select="$rdoc//a[upper-case(@b)=upper-case($hex)][empty(@bactxt)]"/>
							<tr>
								<xsl:if test="exists($sil) and exists($rob) and (count(tokenize($sil/@u,'\s+'))=count(tokenize($rob/@u,'\s+'))) and (some $i in (1 to count(tokenize($sil/@u,'\s+'))) satisfies (int:parseInt((tokenize($sil/@u,'\s+'))[$i],16)!=int:parseInt((tokenize($rob/@u,'\s+'))[$i],16)))">
									<xsl:attribute name="class" select="'warn'"/>
								</xsl:if>
								<td class="int">
									<xsl:value-of select="$code"/>
								</td>
								<td class="hex">
									<xsl:value-of select="$hex"/>
								</td>
								<td class="ipa">
									<xsl:text>&#160;&#160;</xsl:text>
									<xsl:text disable-output-escaping="yes">&amp;#x</xsl:text>
									<xsl:value-of select="$hex"/>
									<xsl:text>;</xsl:text>
									<xsl:text>&#160;&#160;</xsl:text>
								</td>
								<xsl:call-template name="map">
									<xsl:with-param name="code" select="$rob"/>
									<xsl:with-param name="map"  select="'rob'"/>
								</xsl:call-template>
								<td class="int">
									<xsl:value-of select="$code"/>
								</td>
								<td class="hex">
									<xsl:value-of select="$hex"/>
								</td>
								<td class="ipa93">
									<xsl:text>&#160;&#160;</xsl:text>
									<xsl:text disable-output-escaping="yes">&amp;#x</xsl:text>
									<xsl:value-of select="$hex"/>
									<xsl:text>;</xsl:text>
									<xsl:text>&#160;&#160;</xsl:text>
								</td>
								<xsl:call-template name="map">
									<xsl:with-param name="code" select="$sil"/>
									<xsl:with-param name="map"  select="'sil'"/>
								</xsl:call-template>
							</tr>
						</xsl:for-each>
					</tbody>
				</table>
			</body>
		</html>
	</xsl:template>

</xsl:stylesheet>
