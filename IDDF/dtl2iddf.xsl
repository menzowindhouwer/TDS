<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
  xmlns:iddf="http://languagelink.let.uu.nl/tds/ns/iddf" xmlns:xhtml="http://www.w3.org/1999/xhtml"
  xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
  xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
  xmlns:onto="http://languagelink.let.uu.nl/tds/ontology/LinguisticOntology.owl#"
  xmlns:xs="http://www.w3.org/2001/XMLSchema-instance" xmlns:saxon="http://saxon.sf.net/"
  exclude-result-prefixes="owl rdf rdfs onto saxon">

  <xsl:output method="xml" encoding="utf-8"/>

  <xsl:variable name="doc" select="/"/>

  <xsl:variable name="onto" select="document('TDS-Ontology.owl')"/>

  <xsl:key name="concepts" match="owl:Class" use="substring-after(@rdf:about,'#')"/>

  <xsl:key name="datatypes" match="datatype" use="upper-case(@id)"/>
  <xsl:key name="notion" match="notion" use="@id"/>
  <xsl:key name="root" match="/warehouse/data/*" use="concat(@base,'-',@key)"/>

  <xsl:template match="text()"/>

  <!-- copy, taking care of XHTML -->

  <xsl:template match="@*|node()" mode="copy">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()" mode="copy"/>
    </xsl:copy>
  </xsl:template>

  <xsl:template match="text()[normalize-space(.)='']" mode="copy"/>

  <!-- handle (TDS) XHTML in the documentation section -->

  <!--
  <xsl:template match="p" mode="xhtml">
    <xsl:apply-templates select="node()" mode="xhtml"/>
  </xsl:template>
-->

  <xsl:template match="xhtml:tidy" mode="xhtml">
    <xsl:apply-templates mode="xhtml"/>
  </xsl:template>

  <xsl:template match="xhtml:html" mode="xhtml">
    <xsl:apply-templates mode="xhtml"/>
  </xsl:template>

  <xsl:template match="xhtml:body" mode="xhtml">
    <xsl:apply-templates mode="xhtml"/>
  </xsl:template>

  <xsl:template match="xhtml:*" mode="xhtml">
    <xsl:element name="xhtml:{local-name()}">
      <xsl:apply-templates select="@*|node()" mode="xhtml"/>
    </xsl:element>
  </xsl:template>

  <xsl:template match="@xhtml:*" mode="xhtml">
    <xsl:attribute name="xhtml:{local-name()}" select="."/>
  </xsl:template>

  <xsl:template match="@*" mode="xhtml">
    <xsl:attribute name="{local-name()}" select="."/>
  </xsl:template>

  <xsl:template match="qv" mode="xhtml">
    <xhtml:span style="font-style:italic;">
      <xsl:apply-templates select="node()" mode="xhtml"/>
    </xhtml:span>
  </xsl:template>

  <xsl:template match="reference" mode="xhtml">
    <xsl:choose>
      <xsl:when test="exists(@id) and (@type='bib' or empty(@type))">
        <iddf:link rel="bibref" res="bib" ref="{@id}">
          <xsl:value-of select="."/>
        </iddf:link>
      </xsl:when>
      <xsl:when test="exists(@href) and (@type='link' or empty(@type))">
        <iddf:link rel="website" href="{@href}">
          <xsl:value-of select="."/>
        </iddf:link>
      </xsl:when>
      <xsl:otherwise>
        <xsl:apply-templates select="node()" mode="xhtml"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="*" mode="xhtml">
    <xsl:element name="xhtml:{local-name()}">
      <xsl:apply-templates select="@*|node()" mode="xhtml"/>
    </xsl:element>
  </xsl:template>

  <!-- generate IDDF for the TDS warehouse -->

  <xsl:function name="iddf:datatype" saxon:memo-function="yes">
    <xsl:param name="datatype"/>
    <xsl:variable name="base" select="key('datatypes',upper-case($datatype),$doc)/@base"/>
    <xsl:choose>
      <xsl:when test="$base">
        <xsl:sequence select="string-join((iddf:datatype($base),upper-case($datatype)),' ')"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:sequence select="upper-case($datatype)"/>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:function>

  <xsl:template match="warehouse">
    <iddf:iddf iddfVersion="1.0">
      <xsl:apply-templates select="meta"/>
      <xsl:apply-templates select="data"/>
      <iddf:resource xml:id="bib" href="./TDS-Bibliography.xml"/>
      <iddf:resource xml:id="onto" href="./TDS-Ontology.owl"/>
    </iddf:iddf>
  </xsl:template>

  <xsl:template match="meta">
    <iddf:documentation>
      <xsl:call-template name="markers"/>
      <iddf:annotation xml:id="comment">
        <xsl:call-template name="annotations"/>
      </iddf:annotation>
      <iddf:relation xml:id="website">
        <iddf:relation xml:id="homepage"/>
      </iddf:relation>
      <iddf:relation xml:id="person">
        <iddf:relation xml:id="researcher"/>
        <iddf:relation xml:id="contact"/>
      </iddf:relation>
      <iddf:relation xml:id="bibref"/>
      <iddf:relation xml:id="concept">
        <iddf:relation xml:id="givesValues"/>
        <iddf:relation xml:id="assertsExistence"/>
        <iddf:relation xml:id="attributeOf"/>
        <iddf:relation xml:id="notAttributeOf"/>
      </iddf:relation>
      <iddf:relation xml:id="datcat"/>
      <xsl:call-template name="datatype"/>
      <xsl:variable name="nested-scopes" select=".//scope/@ref"/>
      <xsl:apply-templates select="scope[empty(index-of($nested-scopes,@xml:id))]" mode="meta"/>
      <xsl:apply-templates select="dictionary/notion" mode="meta"/>
    </iddf:documentation>
  </xsl:template>

  <xsl:template match="node()" mode="meta"/>

  <xsl:template name="markers">
    <xsl:if test="exists(.//marker)">
      <iddf:annotation xml:id="marker">
        <iddf:values coverage="total">
          <xsl:for-each select=".//marker">
            <iddf:value xml:id="m{@id}">
              <iddf:literal>
                <xsl:value-of select="@id"/>
              </iddf:literal>
              <xsl:apply-templates mode="meta"/>
            </iddf:value>
          </xsl:for-each>
        </iddf:values>
      </iddf:annotation>
    </xsl:if>
  </xsl:template>

  <xsl:template name="annotations">
    <xsl:for-each-group select=".//notion[@type='annotation']" group-by="@name">
      <iddf:annotation xml:id="{current-grouping-key()}"/>
    </xsl:for-each-group>
  </xsl:template>

  <xsl:template name="datatype">
    <xsl:apply-templates select=".//datatype[normalize-space(@base)='']" mode="meta"/>
  </xsl:template>

  <xsl:template match="datatype" mode="meta">
    <iddf:datatype xml:id="{upper-case(@id)}">
      <xsl:apply-templates mode="meta"/>
      <xsl:apply-templates select="../datatype[upper-case(@base)=upper-case(current()/@id)]"
        mode="meta"/>
    </iddf:datatype>
  </xsl:template>

  <xsl:template name="scope">
    <iddf:scope xml:id="{@xml:id}">
      <!-- the DTL can contain a namespace, but it doesn't end up in the warehouse document -->
      <xsl:choose>
        <xsl:when test="exists(namespace)">
          <xsl:attribute name="ns" select="namespace"/>
        </xsl:when>
        <xsl:when test="exists(website)">
          <xsl:attribute name="ns" select="website"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="ns">
            <xsl:text>http://languagelink.let.uu.nl/tds/iddf/ns/</xsl:text>
            <xsl:value-of select="@xml:id"/>
          </xsl:attribute>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:choose>
        <xsl:when test="@type='warehouse'">
          <xsl:attribute name="type" select="'collection'"/>
        </xsl:when>
        <xsl:when test="@type='database'">
          <xsl:attribute name="type" select="'datasource'"/>
        </xsl:when>
      </xsl:choose>
      <xsl:apply-templates select="name|description|website|researcher|contact|scope" mode="meta"/>
    </iddf:scope>
  </xsl:template>

  <xsl:template match="scope" mode="meta">
    <xsl:call-template name="scope"/>
  </xsl:template>

  <xsl:template match="scope/scope" mode="meta">
    <xsl:apply-templates select="ancestor::meta//scope[@xml:id=current()/@ref]" mode="meta"/>
  </xsl:template>

  <xsl:template match="label[normalize-space(.)!='']" mode="meta">
    <iddf:label>
      <xsl:value-of select="."/>
    </iddf:label>
  </xsl:template>

  <xsl:template match="description[normalize-space(.)!='']" mode="meta">
    <iddf:description>
      <xsl:apply-templates select="node()" mode="xhtml"/>
    </iddf:description>
  </xsl:template>

  <xsl:template match="name[normalize-space(.)!='']" mode="meta">
    <iddf:label>
      <xsl:copy-of select="node()"/>
    </iddf:label>
  </xsl:template>

  <xsl:template match="website[normalize-space(.)!='']" mode="meta">
    <iddf:link rel="website" type="homepage" href="{.}"/>
  </xsl:template>

  <xsl:template match="researcher|contact" mode="meta">
    <iddf:link rel="person" type="{local-name()}">
      <xsl:value-of select="."/>
    </iddf:link>
  </xsl:template>

  <xsl:template match="notion[exists(@id)][@abstract!='true']" mode="meta">
    <iddf:notion xml:id="n{@id}" scope="{@scope}" name="{@name}" freq="{@instantiations}">
      <xsl:if test="normalize-space(@scopes)!=''">
        <xsl:attribute name="srcs" select="@scopes"/>
      </xsl:if>
      <xsl:if test="@type='root' or @type='top'">
        <xsl:attribute name="type" select="@type"/>
      </xsl:if>
      <xsl:if test="@data='false' and @datatype!='ENUM' and @datatype!='FREE'">
        <xsl:attribute name="datatype" select="upper-case(@datatype)"/>
      </xsl:if>
      <xsl:apply-templates select="* except notion" mode="meta"/>
      <!--keys-->
      <xsl:if test="exists(key)">
        <iddf:keys datatype="ENUM" coverage="total">
          <xsl:for-each select="key">
            <iddf:key xml:id="{generate-id(.)}">
              <iddf:literal>
                <xsl:apply-templates select="this/node()" mode="xhtml"/>
              </iddf:literal>
              <xsl:apply-templates mode="meta"/>
            </iddf:key>
          </xsl:for-each>
        </iddf:keys>
      </xsl:if>
      <!--values-->
      <xsl:choose>
        <xsl:when test="exists(value)">
          <iddf:values datatype="{upper-case(@datatype)}">
            <xsl:if test="@enum='true'">
              <xsl:attribute name="coverage" select="'total'"/>
            </xsl:if>
            <xsl:for-each select="value">
              <iddf:value xml:id="{generate-id(.)}">
                <iddf:literal>
                  <xsl:apply-templates select="this/node()" mode="xhtml"/>
                </iddf:literal>
                <xsl:apply-templates mode="meta"/>
              </iddf:value>
            </xsl:for-each>
          </iddf:values>
        </xsl:when>
        <xsl:when test="@data='true'">
          <xsl:choose>
            <xsl:when test="exists(@datatype)">
              <iddf:values datatype="{upper-case(@datatype)}"/>
            </xsl:when>
            <xsl:otherwise>
              <iddf:values datatype="FREE"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:when>
      </xsl:choose>
      <xsl:apply-templates select="notion" mode="meta"/>
    </iddf:notion>
  </xsl:template>

  <xsl:template match="notion[exists(@ref)]" mode="meta">
    <iddf:notion ref="n{@ref}"/>
  </xsl:template>

  <xsl:template match="datcat" mode="meta">
    <iddf:link rel="datcat" href="{.}"/>
  </xsl:template>

  <xsl:template match="link" mode="meta">
    <xsl:choose>
      <xsl:when test="exists(@concept)">
        <xsl:variable name="concept" select="@concept"/>
        <xsl:variable name="type" select="@type"/>
        <xsl:variable name="onto-concept" select="key('concepts',$concept,$onto)"/>
        <xsl:if test="$onto-concept">
          <iddf:link rel="concept" res="onto" ref="{$concept}">
            <xsl:if test="$type">
              <xsl:attribute name="type" select="$type"/>
            </xsl:if>
            <xsl:choose>
              <xsl:when test="normalize-space($onto-concept/rdfs:label)">
                <xsl:value-of select="replace(normalize-space($onto-concept/rdfs:label),' \*$','')"
                />
              </xsl:when>
              <xsl:otherwise>
                <xsl:value-of select="$concept"/>
              </xsl:otherwise>
            </xsl:choose>
          </iddf:link>
          <xsl:for-each select="$onto-concept/onto:label-alias">
            <iddf:link rel="concept" res="onto" ref="{$concept}">
              <xsl:if test="$type">
                <xsl:attribute name="type" select="$type"/>
              </xsl:if>
              <xsl:value-of select="replace(normalize-space(.),' \*$','')"/>
            </iddf:link>
          </xsl:for-each>
        </xsl:if>
      </xsl:when>
      <!-- TODO: notion links should be resolved
        <xsl:when test="exists(@notion)">
          <xsl:attribute name="type" select="'notion'"/>
          <xsl:if test="@type">
            <xsl:attribute name="rel" select="@type"/>
          </xsl:if>
          <!- - shouldn't this be @ref? - ->
          <xsl:attribute name="ref">
            <xsl:text>n</xsl:text>
            <xsl:value-of select="@notion"/>
          </xsl:attribute>
        </xsl:when>
        -->
    </xsl:choose>
  </xsl:template>

  <xsl:template match="data">
    <iddf:data>
      <xsl:for-each select="/warehouse/meta//scope[exists(@xml:id)]">
        <xsl:namespace name="{@xml:id}">
          <xsl:choose>
            <xsl:when test="exists(namespace)">
              <xsl:value-of select="namespace"/>
            </xsl:when>
            <xsl:when test="exists(website)">
              <xsl:value-of select="website"/>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="concat('http://languagelink.let.uu.nl/tds/iddf/ns/',@xml:id)"/>
            </xsl:otherwise>
          </xsl:choose>
        </xsl:namespace>
      </xsl:for-each>
      <xsl:apply-templates mode="data"/>
    </iddf:data>
  </xsl:template>

  <xsl:template match="*" mode="data">
    <xsl:variable name="node" select="current()"/>
    <xsl:variable name="scope" select="/warehouse/meta//scope[@xml:id=current()/@scope]"/>
    <xsl:variable name="ns">
      <xsl:choose>
        <xsl:when test="exists($scope/namespace)">
          <xsl:value-of select="$scope/namespace"/>
        </xsl:when>
        <xsl:when test="exists($scope/website)">
          <xsl:value-of select="$scope/website"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="concat('http://languagelink.let.uu.nl/tds/iddf/ns/',$scope/@xml:id)"
          />
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:element name="{@scope}:{local-name()}" namespace="{$ns}">
      <xsl:attribute name="iddf:notion">
        <xsl:text>n</xsl:text>
        <xsl:value-of select="@base"/>
      </xsl:attribute>
      <xsl:if test="exists(@key)">
        <xsl:attribute name="key" select="@key"/>
        <xsl:variable name="notion" select="key('notion',@base,$doc)"/>
        <xsl:variable name="enumkey" select="$notion/key[this = current()/@key]"/>
        <xsl:attribute name="iddf:key" select="generate-id($enumkey)"/>
      </xsl:if>
      <xsl:if test="exists(@ref)">
        <xsl:attribute name="ref" select="@ref"/>
        <xsl:attribute name="iddf:ref"
          select="concat('in',generate-id(key('root',concat(@base,'-',@ref))))"/>
      </xsl:if>
      <xsl:if test="empty(@ref)">
        <xsl:attribute name="xml:id" select="concat('in',generate-id(.))"/>
      </xsl:if>
      <xsl:attribute name="iddf:srcs" select="@srcs"/>
      <!-- no values, but only annotations -->
      <xsl:variable name="ann"
        select="*[@type='annotation'][empty(value[@src=current()/value/@src])]"/>
      <xsl:if test="exists($ann)">
        <xsl:variable name="val" select="generate-id(.)"/>
        <iddf:value xml:id="iv{$val}" ann="a{$val}" xs:nil="true"
          srcs="{string-join(distinct-values($ann/tokenize(@srcs,'\s+')),' ')}"/>
        <xsl:for-each select="$ann/value">
          <iddf:annotation xml:id="ia{generate-id(.)}" ann="a{$val}"
            type="comment {local-name(parent::*)}" srcs="{@src}">
            <xsl:apply-templates select="this/node()" mode="xhtml"/>
          </iddf:annotation>
        </xsl:for-each>
      </xsl:if>
      <!-- values with a value (a this) -->
      <xsl:for-each-group select="value" group-by="this">
        <xsl:call-template name="data-value">
          <xsl:with-param name="vals" select="current-group()"/>
          <xsl:with-param name="anns"
            select="$node/*[@type='annotation'][exists(value[@src=current-group()/@src])]"/>
        </xsl:call-template>
      </xsl:for-each-group>
      <!-- values with only a marker (no this) -->
      <xsl:if test="exists(value[empty(this)])">
        <xsl:call-template name="data-value">
          <xsl:with-param name="vals" select="value[empty(this)]"/>
          <xsl:with-param name="anns"
            select="$node/*[@type='annotation'][exists(value[@src=current-group()/@src])]"/>
        </xsl:call-template>
      </xsl:if>
      <xsl:apply-templates select="* except value" mode="data"/>
    </xsl:element>
  </xsl:template>

  <xsl:template name="data-value">
    <xsl:param name="vals"/>
    <xsl:param name="anns"/>
    <xsl:variable name="val" select="$vals[1]"/>
    <xsl:variable name="ann" select="exists($vals/mark) or exists($anns)"/>
    <iddf:value xml:id="iv{generate-id($val)}" srcs="{string-join(distinct-values($vals/@src),' ')}">
      <xsl:variable name="notion" select="key('notion',$val/../@base,$doc)"/>
      <xsl:variable name="datatype" select="$notion/@datatype"/>
      <xsl:if test="$datatype">
        <xsl:attribute name="datatype">
          <xsl:value-of select="iddf:datatype($datatype)"/>
        </xsl:attribute>
      </xsl:if>
      <xsl:if test="$ann">
        <xsl:attribute name="ann" select="concat('a',generate-id($val))"/>
      </xsl:if>
      <xsl:choose>
        <xsl:when test="exists($val/this)">
          <xsl:variable name="enumval" select="$notion/value[this = $val/this]"/>
          <xsl:if test="exists($enumval)">
            <xsl:attribute name="val" select="generate-id($enumval)"/>
          </xsl:if>
          <xsl:apply-templates select="$val/this/node()" mode="xhtml"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:attribute name="xs:nil" select="'true'"/>
        </xsl:otherwise>
      </xsl:choose>
    </iddf:value>
    <xsl:for-each-group select="$vals" group-by="mark">
      <xsl:variable name="mark" select="current-group()[1]/mark"/>
      <iddf:annotation xml:id="ia{generate-id($mark)}" ann="a{generate-id($val)}" type="marker"
        val="m{$mark}" srcs="{string-join(distinct-values(current-group()/@src),' ')}">
        <xsl:value-of select="$mark"/>
      </iddf:annotation>
    </xsl:for-each-group>
    <xsl:for-each select="$anns/value">
      <iddf:annotation xml:id="ia{generate-id(.)}v{generate-id($val)}" ann="a{generate-id($val)}"
        type="comment {local-name(parent::*)}" srcs="{@src}">
        <xsl:apply-templates select="this/node()" mode="xhtml"/>
      </iddf:annotation>
    </xsl:for-each>
  </xsl:template>

</xsl:stylesheet>
