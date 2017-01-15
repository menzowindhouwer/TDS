<?xml version="1.0"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
  xmlns:iddf="http://languagelink.let.uu.nl/tds/ns/iddf"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  xmlns:xs="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:saxon="http://saxon.sf.net/"
  exclude-result-prefixes="saxon"
>

  <xsl:output method="xml" encoding="utf-8"/>
  
  <xsl:variable name="doc" select="/"/>

  <!-- identity copy -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- documentation section -->
  
  <!-- * add a label which is equal to the literal, i.e., there is always a label no need to fallback to the literal -->
  
  <!-- ** add label to various IDDF building blocks -->
  <xsl:template match="/iddf:iddf/iddf:documentation//iddf:datatype|/iddf:iddf/iddf:documentation//iddf:annotation|/iddf:iddf/iddf:documentation//iddf:relation|/iddf:iddf/iddf:documentation//iddf:scope">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:if test="empty(iddf:label)">
        <iddf:label>
          <xsl:value-of select="@xml:id"/>
        </iddf:label>
      </xsl:if>
      <xsl:apply-templates select="node()"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- ** add label to notions -->
  <xsl:template match="/iddf:iddf/iddf:documentation//iddf:notion[exists(@xml:id)]">
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:if test="empty(iddf:label)">
        <iddf:label>
          <xsl:value-of select="@name"/>
        </iddf:label>
      </xsl:if>
      <xsl:apply-templates select="node()"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- ** add label to (key) values -->
  <xsl:template match="/iddf:iddf/iddf:documentation//iddf:literal">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
    <xsl:if test="empty(preceding-sibling::iddf:label|following-sibling::iddf:label)">
      <iddf:label>
        <xsl:value-of select="."/>
      </iddf:label>
    </xsl:if>
  </xsl:template>
  

</xsl:stylesheet>
