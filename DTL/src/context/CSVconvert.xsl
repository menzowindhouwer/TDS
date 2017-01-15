<?xml version="1.0"?>
<xsl:stylesheet
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  version="2.0"
>

    <xsl:output method="xml" encoding="utf-8"/>

    <xsl:param name="header" select="'false'"/>
    <xsl:param name="table"  select="'csv'"/>
    <xsl:param name="resource" select="'tds'"/>
    
    <xsl:variable name="headerrow" select="(csvFile/line)[1]"/>

    <xsl:variable name="doc" select="/"/>
    
    <xsl:template match="csvFile">
        <database>
            <answer table="{$table}" resource="{$resource}">
                <xsl:choose>
                    <xsl:when test="$header='true'">
                        <xsl:apply-templates select="subsequence(line,2)"/>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:apply-templates/>
                    </xsl:otherwise>
               </xsl:choose>
            </answer>
        </database>
    </xsl:template>
    
    <xsl:template match="line">
        <tuple row="{count(preceding-sibling::line) + (if ($header='true') then (0) else (1))}">
            <xsl:apply-templates/>
        </tuple>
    </xsl:template>
    
    <xsl:template match="value">
        <xsl:variable name="col" select="count(preceding-sibling::value) + 1"/>
        <xsl:variable name="name" select="
            if   (($header='true') and exists(($headerrow/value)[$col]) and (normalize-space(($headerrow/value)[$col])!=''))
            then (normalize-space(($headerrow/value)[$col]))
            else (concat('field_',$col))
        "/>
        <xsl:element name="{$name}">
            <xsl:value-of select="."/>
        </xsl:element>
    </xsl:template>
    
</xsl:stylesheet>

