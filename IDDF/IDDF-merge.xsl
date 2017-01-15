<?xml version="1.0"?>
<xsl:stylesheet
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
  xmlns:iddf="http://languagelink.let.uu.nl/tds/ns/iddf"
  xmlns:xhtml="http://www.w3.org/1999/xhtml"
  xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xmlns:saxon="http://saxon.sf.net/"
  xmlns:functx="http://www.functx.com"
  exclude-result-prefixes="saxon xs xsi functx"
>

  <xsl:output method="xml" encoding="utf-8"/>
  
  <xsl:variable name="add" select="/"/>
  
  <xsl:param name="main-uri" select="resolve-uri('TDS-IDDF.xml',base-uri($add))"/>
  <xsl:param name="main" select="if (exists($main-uri)) then (doc($main-uri)) else ()"/>
  
  <!-- functx -->
  <xsl:function name="functx:value-except" as="xs:anyAtomicType*">
    <xsl:param name="arg1" as="xs:anyAtomicType*"/>
    <xsl:param name="arg2" as="xs:anyAtomicType*"/>
    <xsl:sequence select="distinct-values($arg1[not(.=$arg2)])"/>
  </xsl:function>
  
  <!-- identity copy -->
  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
    </xsl:copy>
  </xsl:template>
  
  <!-- keys -->
  <xsl:template match="iddf:key">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:variable name="lit" select="iddf:literal"/>
    <xsl:variable name="key" select="$addContext/iddf:key[iddf:literal=$lit]"/>
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <!-- new links -->
      <xsl:for-each select="functx:value-except($key/iddf:link,iddf:link)">
        <xsl:variable name="link" select="current()"/>
        <xsl:copy-of select="$key/iddf:link[.=$link]"/>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template name="keys">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="* except iddf:key"/>
      <!-- existing values -->
      <xsl:apply-templates select="iddf:key">
        <xsl:with-param name="addContext" select="$addContext"/>
      </xsl:apply-templates>
      <!-- new values -->
      <xsl:for-each select="functx:value-except($addContext/iddf:key/iddf:literal,iddf:key/iddf:literal)">
        <xsl:variable name="lit" select="current()"/>
        <xsl:copy-of select="$addContext/iddf:key[iddf:literal=$lit]"/>
      </xsl:for-each>
    </xsl:copy>    
  </xsl:template>
  
  <!-- values -->
  <xsl:template match="iddf:value">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:variable name="lit" select="iddf:literal"/>
    <xsl:variable name="val" select="$addContext/iddf:value[iddf:literal=$lit]"/>
    <xsl:copy>
      <xsl:apply-templates select="@*|node()"/>
      <!-- new links -->
      <xsl:for-each select="functx:value-except($val/iddf:link,iddf:link)">
        <xsl:variable name="link" select="current()"/>
        <xsl:copy-of select="$val/iddf:link[.=$link]"/>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template name="values">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="* except iddf:value"/>
      <!-- existing values -->
      <xsl:apply-templates select="iddf:value">
        <xsl:with-param name="addContext" select="$addContext"/>
      </xsl:apply-templates>
      <!-- new values -->
      <xsl:for-each select="functx:value-except($addContext/iddf:value/iddf:literal,iddf:value/iddf:literal)">
        <xsl:variable name="lit" select="current()"/>
        <xsl:copy-of select="$addContext/iddf:value[iddf:literal=$lit]"/>
      </xsl:for-each>
    </xsl:copy>    
  </xsl:template>

  <!-- annotations -->
  <xsl:template match="iddf:annotation">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:variable name="id" select="@xml:id"/>
    <xsl:variable name="ann" select="$addContext/iddf:annotation[@xml:id=$id]"/>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="* except iddf:annotation except iddf:values"/>
      <xsl:for-each select="iddf:values">
        <xsl:call-template name="values">
          <xsl:with-param name="addContext" select="$ann/iddf:values"/>
        </xsl:call-template>
      </xsl:for-each>
      <xsl:call-template name="annotations">
        <xsl:with-param name="addContext" select="$ann"/>
      </xsl:call-template>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template name="annotations">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <!-- existing annotations -->
    <xsl:apply-templates select="iddf:annotation">
      <xsl:with-param name="addContext" select="$addContext"/>
    </xsl:apply-templates>
    <!-- new annotations -->
    <xsl:for-each select="functx:value-except($addContext/iddf:annotation/@xml:id,iddf:annotation/@xml:id)">
      <xsl:variable name="id" select="current()"/>
      <xsl:copy-of select="$addContext/iddf:annotation[@xml:id=$id]"/>
    </xsl:for-each>
  </xsl:template>
  
  <!-- relations -->
  <xsl:template match="iddf:relation">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:variable name="id" select="@xml:id"/>
    <xsl:variable name="rel" select="$addContext/iddf:relation[@xml:id=$id]"/>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="* except iddf:relation"/>
      <xsl:call-template name="relations">
        <xsl:with-param name="addContext" select="$rel"/>
      </xsl:call-template>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template name="relations">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <!-- existing relations -->
    <xsl:apply-templates select="iddf:relation">
      <xsl:with-param name="addContext" select="$addContext"/>
    </xsl:apply-templates>
    <!-- new relations -->
    <xsl:for-each select="functx:value-except($addContext/iddf:relation/@xml:id,iddf:relation/@xml:id)">
      <xsl:variable name="id" select="current()"/>
      <xsl:copy-of select="$addContext/iddf:relation[@xml:id=$id]"/>
    </xsl:for-each>
  </xsl:template>
  
  <!-- datatypes -->
  <xsl:template match="iddf:datatype">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:variable name="id" select="@xml:id"/>
    <xsl:variable name="dt" select="$addContext/iddf:datatype[@xml:id=$id]"/>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="* except iddf:datatype except iddf:values"/>
      <xsl:for-each select="iddf:values">
        <xsl:call-template name="values">
          <xsl:with-param name="addContext" select="$dt/iddf:values"/>
        </xsl:call-template>
      </xsl:for-each>
      <xsl:call-template name="datatypes">
        <xsl:with-param name="addContext" select="$dt"/>
      </xsl:call-template>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template name="datatypes">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <!-- existing datatypes -->
    <xsl:apply-templates select="iddf:datatype">
      <xsl:with-param name="addContext" select="$addContext"/>
    </xsl:apply-templates>
    <!-- new datatypes -->
    <xsl:for-each select="functx:value-except($addContext/iddf:datatype/@xml:id,iddf:datatype/@xml:id)">
      <xsl:variable name="id" select="current()"/>
      <xsl:copy-of select="$addContext/iddf:datatype[@xml:id=$id]"/>
    </xsl:for-each>
  </xsl:template>
  
  <!-- scopes -->
  <xsl:template match="iddf:scope">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:variable name="id" select="@xml:id"/>
    <xsl:variable name="sc" select="$addContext/iddf:scope[@xml:id=$id]"/>
    <xsl:copy>
      <xsl:apply-templates select="@*"/>
      <xsl:apply-templates select="* except iddf:scope"/>
      <xsl:call-template name="scopes">
        <xsl:with-param name="addContext" select="$sc"/>
      </xsl:call-template>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template name="scopes">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <!-- existing scopes -->
    <xsl:apply-templates select="iddf:scope">
      <xsl:with-param name="addContext" select="$addContext"/>
    </xsl:apply-templates>
    <!-- new scopes -->
    <xsl:for-each select="functx:value-except($addContext/iddf:scope/@xml:id,iddf:scope/@xml:id)">
      <xsl:variable name="id" select="current()"/>
      <xsl:copy-of select="$addContext/iddf:scope[@xml:id=$id]"/>
    </xsl:for-each>
  </xsl:template>
  
  <!-- notions -->
  <xsl:template match="iddf:notion/@freq">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:attribute name="freq">
      <xsl:choose>
        <xsl:when test="exists($addContext)">
          <xsl:value-of select=". + $addContext/@freq"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="."/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:template>
  
  <xsl:template match="iddf:notion/@srcs">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:attribute name="srcs" select="string-join((.,$addContext/@srcs),' ')"/>
  </xsl:template>
  
  <xsl:template match="iddf:notion">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <xsl:variable name="scope" select="@scope"/>
    <xsl:variable name="name" select="@name"/>
    <xsl:variable name="n" select="$addContext/iddf:notion[@scope=$scope][@name=$name]"/>
    <xsl:copy>
      <xsl:apply-templates select="@*">
        <xsl:with-param name="addContext" select="$n"/>
      </xsl:apply-templates>
      <xsl:apply-templates select="* except iddf:notion except iddf:values except iddf:values"/>
      <xsl:for-each select="iddf:keys">
        <xsl:call-template name="keys">
          <xsl:with-param name="addContext" select="$n/iddf:keys"/>
        </xsl:call-template>
      </xsl:for-each>
      <xsl:for-each select="iddf:values">
        <xsl:call-template name="values">
          <xsl:with-param name="addContext" select="$n/iddf:values"/>
        </xsl:call-template>
      </xsl:for-each>
      <xsl:call-template name="notions">
        <xsl:with-param name="addContext" select="$n"/>
      </xsl:call-template>
    </xsl:copy>
  </xsl:template>
  
  <xsl:template name="notions">
    <xsl:param name="addContext" select="()" as="node()?"/>
    <!-- existing notions -->
    <xsl:apply-templates select="iddf:notion">
      <xsl:with-param name="addContext" select="$addContext"/>
    </xsl:apply-templates>
    <!-- new notions -->
    <xsl:for-each select="functx:value-except($addContext/iddf:annotation/@xml:id,iddf:annotation/@xml:id)">
      <xsl:variable name="id" select="current()"/>
      <xsl:copy-of select="$addContext/iddf:annotation[@xml:id=$id]"/>
    </xsl:for-each>
  </xsl:template>
  
  <!-- main -->
  <xsl:template match="/">
    <!-- check versions -->
    <xsl:if test="$main/iddf:iddf/@iddfVersion!=$add/iddf:iddf/@iddfVersion">
      <xsl:message terminate="yes">FTL: IDDF versions are not the same! [<xsl:value-of select="$main/iddf:iddf/@iddfVersion"/>]!=[<xsl:value-of select="$add/iddf:iddf/@iddfVersion"/>]</xsl:message>
    </xsl:if>
    <xsl:if test="$main/iddf:iddf/@iddfVersion!='1.0'">
      <xsl:message terminate="yes">FTL: merge can not handle IDDF version[<xsl:value-of select="$main/iddf:iddf/@iddfVersion"/>]!</xsl:message>
    </xsl:if>
    <xsl:if test="$add/iddf:iddf/@iddfVersion!='1.0'">
      <xsl:message terminate="yes">FTL: merge can not handle IDDF version[<xsl:value-of select="$add/iddf:iddf/@iddfVersion"/>]!</xsl:message>
    </xsl:if>
    <iddf:iddf iddfVersion="1.0">
      <iddf:documentation>
        <xsl:for-each select="$main/iddf:iddf/iddf:documentation">
          <!-- annotations -->
          <xsl:call-template name="annotations">
            <xsl:with-param name="addContext" select="$add/iddf:iddf/iddf:documentation"/>
          </xsl:call-template>
          <!-- relations -->
          <xsl:call-template name="relations">
            <xsl:with-param name="addContext" select="$add/iddf:iddf/iddf:documentation"/>
          </xsl:call-template>
          <!-- datatypes -->
          <xsl:call-template name="datatypes">
            <xsl:with-param name="addContext" select="$add/iddf:iddf/iddf:documentation"/>
          </xsl:call-template>
          <!-- scopes -->
          <xsl:if test="$main/iddf:iddf/iddf:documentation/iddf:scope/@xml:id!=$add/iddf:iddf/iddf:documentation/iddf:scope/@xml:id">
            <xsl:message terminate="yes">FTL: IDDF collections are not the same! [<xsl:value-of select="$main/iddf:iddf/iddf:documentation/iddf:scope/@xml:id"/>]!=[<xsl:value-of select="$add/iddf:iddf/iddf:documentation/iddf:scope/@xml:id"/>]</xsl:message>
          </xsl:if>
          <xsl:call-template name="scopes">
            <xsl:with-param name="addContext" select="$add/iddf:iddf/iddf:documentation"/>
          </xsl:call-template>
          <!-- notions -->
          <xsl:call-template name="notions">
            <xsl:with-param name="addContext" select="$add/iddf:iddf/iddf:documentation"/>
          </xsl:call-template>
        </xsl:for-each>        
      </iddf:documentation>
    </iddf:iddf>
  </xsl:template>
  
</xsl:stylesheet>
