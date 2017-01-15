<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0"
    xmlns:iddq="http://languagelink.let.uu.nl/tds/ns/iddq"
    xmlns:iddf="http://languagelink.let.uu.nl/tds/ns/iddf">

    <!-- 
        covered functionality from the 'old' TDS:
        X match value or key value
        X match case sensitive or insensitive
        X match literal and label
        X inverse match (not)
        
        STATUS:
        order-based operations
        - value
          - case sensitive      :val-eq-cs      : ok
            - inverse           :val-eq-cs-inv  : ok
          - case insensitive    :val-eq-cis-    : ok
            - inverse           :val-eq-cis-inv : ok
        - value label
          - case sensitive      :vall-eq-cs     : ok
            - inverse           :vall-eq-cs-inv : ok
          - case insensitive    :vall-eq-cis    : ok
            - inverse           :vall-eq-cis-inv: ok
         - key value
           - case sensitive     :key-eq-cs      : ok
             - inverse          :key-eq-cs-inv  : ok
           - case insensitive   :key-eq-cis     : 
             - inverse          :key-eq=cis-inv : 
         - key value label
           - case sensitive     :keyl-eq-cs     : ok
             - inverse          :keyl-eq-cs-inv : ok
           - case insensitive   :keyl-eq-cis    : ok
             - inverse          :keyl-eq-cis-inv: ok
        regular expression match
        - value
          - case sensitive      :val-re-cs      : ok
            - inverse           :val-re-cs-inv  : ok
          - case insensitive    :val-re-cis     : ok
            - inverse           :val-re-cis-inv : ok
        - value label
          - case sensitive      :vall-re-cs     : ok
            - inverse           :vall-re-cs-inv : ok
          - case insensitive    :vall-re-cis    : ok
            - inverse           :vall-re-cis-inv: ok
        - key value
          - case sensitive      :key-re-cs      : ok
            - inverse           :key-re-cs-inv  : ok
          - case insensitive    :key-re-cis     :
            - inverse           :key-re-cis-inv :
        - key value label
          - case sensitive      :keyl-re-cs     : ok
            - inverse           :keyl-re-cs-inv : ok
          - case insensitive    :keyl-re-cis    : ok
            - inverse           :keyl-re-cis-inv: ok
        substring match
        - value
          - case sensitive      :val-ss-cs      : ok
            - inverse           :val-ss-cs-inv  : ok
          - case insensitive    :val-ss-cis     : ok
            - inverse           :val-ss-cis-inv : ok
        - value label
          - case sensitive      :vall-ss-cs     : ok
            - inverse           :vall-ss-cs-inv : ok
          - case insensitive    :vall-ss-cis    : ok
            - inverse           :vall-ss-cis-inv: ok
        - key value
          - case sensitive      :key-ss-cs      : ok
            - inverse           :key-ss-cs-inv  : ok
          - case insensitive    :key-ss-cis     :
            - inverse           :key-ss-cis-inv :
        - key value label
          - case sensitive      :keyl-ss-cs     : ok
            - inverse           :keyl-ss-cs-inv : ok
          - case insensitive    :keyl-ss-cis    : ok
            - inverse           :keyl-ss-cis-inv: ok
        exist                   :ex             : ok
        - inverse               :ex-inv         : ok
        
    -->

    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:param name="file"/>
    <xsl:param name="labels"/>

    <xsl:variable name="NL" select="system-property('line.separator')"/>

    <xsl:variable name="doc" select="/"/>
    <xsl:variable name="indentStep" select="'  '"/>

    <!-- SELECTION -->

    <xsl:function name="iddq:select">
        <xsl:param name="notion"/>
        <xsl:text>/iddf:iddf/iddf:data/*[@iddf:notion eq '</xsl:text>
        <xsl:value-of select="$notion"/>
        <xsl:text>']</xsl:text>
        <xsl:for-each select="$doc/iddq:query/iddf:notion[@id=$notion]/iddq:select">
            <xsl:text>[</xsl:text>
            <!--
                select all descendant matches and combines, pick the first in document order, and then select only the match
                this should give us the match that isn't in any nested combine
            -->
            <xsl:variable name="match" select="(descendant::iddq:match|descendant::iddq:combine)[1]/self::iddq:match"/>
            <xsl:choose>
                <xsl:when test="$match/@operator='ex'">
                    <!-- existence, the path brings us to the instances -->
                    <!-- TODO: handle @xs:nil='true' -->
                    <xsl:if test="$match/@inverse='true'">
                        <xsl:text>not(</xsl:text>
                    </xsl:if>
                    <xsl:text>exists(</xsl:text>
                    <xsl:apply-templates select="." mode="select"/>
                    <xsl:text>)</xsl:text>
                    <xsl:if test="$match/@inverse='true'">
                        <xsl:text>)</xsl:text>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:apply-templates select="." mode="select"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:text>]</xsl:text>
        </xsl:for-each>
    </xsl:function>

    <xsl:template match="text()" mode="select"/>

    <!-- boolean combination operators -->
    <xsl:template match="iddq:combine[@operator=('and','or')]" mode="select">
        <xsl:param name="prefix" select="''"/>
        <xsl:variable name="operator" select="@operator"/>
        <xsl:text>.[</xsl:text>
        <xsl:if test="@inverse='true'">
            <xsl:text>not(</xsl:text>
        </xsl:if>
        <xsl:for-each select="*">
            <xsl:text>(</xsl:text>
            <xsl:variable name="match" select="descendant-or-self::iddq:match"/>
            <!--
                select all descendant matches and combines, pick the first in document order, and then select only the match
                this should give us the match that isn't in any nested combine
            -->
            <xsl:variable name="match" select="(descendant-or-self::iddq:match|descendant::iddq:combine)[1]/self::iddq:match"/>
            <xsl:choose>
                <xsl:when test="$match/@operator='ex'">
                    <!-- existence, the path brings us to the instances -->
                    <!-- TODO: handle @xs:nil='true' -->
                    <xsl:if test="$match/@inverse='true'">
                        <xsl:text>not(</xsl:text>
                    </xsl:if>
                    <xsl:text>exists(</xsl:text>
                    <xsl:value-of select="$prefix"/>
                    <xsl:apply-templates select="." mode="select"/>
                    <xsl:text>)</xsl:text>
                    <xsl:if test="$match/@inverse='true'">
                        <xsl:text>)</xsl:text>
                    </xsl:if>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$prefix"/>
                    <xsl:apply-templates select="." mode="select"/>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:text>)</xsl:text>
            <xsl:if test="position() != last()">
                <xsl:text> </xsl:text>
                <xsl:value-of select="$operator"/>
                <xsl:text> </xsl:text>
            </xsl:if>
        </xsl:for-each>
        <xsl:if test="@inverse='true'">
            <xsl:text>)</xsl:text>
        </xsl:if>
        <xsl:text>]</xsl:text>
    </xsl:template>
    
    <!-- exist operator is handled in the combine -->
    <xsl:template match="iddq:match[@operator='ex']" mode="select">
        <xsl:text>.</xsl:text>
    </xsl:template>

    <!-- value matching operators -->
    <xsl:template match="iddq:match" mode="select">

        <xsl:text>.[</xsl:text>
        <xsl:if test="@inverse='true'">
            <xsl:text>not(</xsl:text>
        </xsl:if>

        <!-- value-based operators -->
        <!-- TODO: handle @xs:nil='true' -->
        
        <xsl:variable name="cs-val">
            <xsl:value-of select="replace(.,'''','''''')"/>
        </xsl:variable>

        <xsl:variable name="val">
            <xsl:choose>
                <xsl:when test="@case='insensitive'">
                    <xsl:value-of select="lower-case($cs-val)"/>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:value-of select="$cs-val"/>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:variable>

        <!-- (key) value match -->
        <xsl:variable name="kv-cs-target"
            select="if (@target='key') then ('@key') else ('iddf:value')"/>
        <xsl:variable name="kv-target"
            select="if (@case='insensitive') then (concat('lower-case(',$kv-cs-target,')')) else ($kv-cs-target)"/>

        <xsl:choose>
            <xsl:when test="@operator='ss'">

                <xsl:text>contains(</xsl:text>
                <xsl:value-of select="$kv-target"/>
                <xsl:text>,'</xsl:text>
                <xsl:value-of select="$val"/>
                <xsl:text>')</xsl:text>

            </xsl:when>
            <xsl:when test="@operator='re'">

                <!-- for case insensitivity use the i flag, don't mess with the regular expression -->
                <xsl:text>matches(</xsl:text>
                <xsl:value-of select="$kv-cs-target"/>
                <xsl:text>,'</xsl:text>
                <xsl:value-of select="$cs-val"/>
                <xsl:text>'</xsl:text>
                <xsl:if test="@case='insensitive'">
                    <xsl:text>,'i'</xsl:text>
                </xsl:if>
                <xsl:text>)</xsl:text>

            </xsl:when>
            <xsl:otherwise>
                <!-- various order-based operators -->

                <xsl:value-of select="$kv-target"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="@operator"/>
                <xsl:text> '</xsl:text>
                <xsl:value-of select="$val"/>
                <xsl:text>'</xsl:text>

            </xsl:otherwise>
        </xsl:choose>

        <xsl:if test="@labels='true'">

            <xsl:text> or </xsl:text>

            <xsl:choose>
                <xsl:when test="@target='key'">
                    <xsl:variable name="kl-cs-target"
                        select="'id(@iddf:key,$doc)/iddf:label'"/>
                    <xsl:variable name="kl-target"
                        select="if (@case='insensitive') then (concat('lower-case(',$kl-cs-target,')')) else ($kl-cs-target)"/>

                    <xsl:choose>

                        <xsl:when test="@operator='ss'">
                            <xsl:text>contains(</xsl:text>
                            <xsl:value-of select="$kl-target"/>
                            <xsl:text>,'</xsl:text>
                            <xsl:value-of select="$val"/>
                            <xsl:text>')</xsl:text>
                        </xsl:when>

                        <xsl:when test="@operator='re'">
                            <xsl:text>matches(</xsl:text>
                            <xsl:value-of select="$kl-cs-target"/>
                            <xsl:text>,'</xsl:text>
                            <xsl:value-of select="$cs-val"/>
                            <xsl:text>'</xsl:text>
                            <xsl:if test="@case='insensitive'">
                                <xsl:text>,'i'</xsl:text>
                            </xsl:if>
                            <xsl:text>)</xsl:text>
                        </xsl:when>

                        <xsl:otherwise>

                            <xsl:value-of select="$kl-target"/>
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="@operator"/>
                            <xsl:text> '</xsl:text>
                            <xsl:value-of select="$val"/>
                            <xsl:text>'</xsl:text>

                        </xsl:otherwise>

                    </xsl:choose>

                </xsl:when>
                <xsl:otherwise>
                    <xsl:variable name="vl-cs-target"
                        select="'id(iddf:value/@val,$doc)/iddf:label'"/>
                    <xsl:variable name="vl-target"
                        select="if (@case='insensitive') then (concat('lower-case(',$vl-cs-target,')')) else ($vl-cs-target)"/>

                    <xsl:choose>
                        <xsl:when test="@operator='ss'">

                            <xsl:text>contains(</xsl:text>
                            <xsl:value-of select="$vl-target"/>
                            <xsl:text>,'</xsl:text>
                            <xsl:value-of select="$val"/>
                            <xsl:text>')</xsl:text>

                        </xsl:when>
                        <xsl:when test="@operator='re'">

                            <xsl:text>matches(</xsl:text>
                            <xsl:value-of select="$vl-cs-target"/>
                            <xsl:text>,'</xsl:text>
                            <xsl:value-of select="$cs-val"/>
                            <xsl:text>'</xsl:text>
                            <xsl:if test="@case='insensitive'">
                                <xsl:text>,'i'</xsl:text>
                            </xsl:if>
                            <xsl:text>)</xsl:text>
                            
                        </xsl:when>
                        <xsl:otherwise>

                            <xsl:value-of select="$vl-target"/>
                            <xsl:text> </xsl:text>
                            <xsl:value-of select="@operator"/>
                            <xsl:text> '</xsl:text>
                            <xsl:value-of select="$val"/>
                            <xsl:text>'</xsl:text>

                        </xsl:otherwise>

                    </xsl:choose>

                </xsl:otherwise>
            </xsl:choose>

        </xsl:if>

        <xsl:if test="@inverse='true'">
            <xsl:text>)</xsl:text>
        </xsl:if>
        <xsl:text>]</xsl:text>

    </xsl:template>

    <!-- walk the path -->
    <xsl:template match="iddf:notion" mode="select">
        <xsl:param name="prefix" select="''"/>
        <xsl:value-of select="$prefix"/>
        <xsl:value-of select="concat(@scope,':',@name)"/>
        <xsl:text>/</xsl:text>
        <xsl:apply-templates mode="select"/>
    </xsl:template>

    <!-- nested root notion needs to be dereferenced -->
    <xsl:template match="iddf:notion[@type='root'][exists(ancestor::iddf:notion)]" mode="select">
        <xsl:param name="prefix" select="''"/>
        <xsl:value-of select="$prefix"/>
        <xsl:value-of select="concat(@scope,':',@name)"/>
        <xsl:text>[id(@iddf:ref,$doc)/</xsl:text>
        <xsl:apply-templates mode="select"/>
        <xsl:text>]</xsl:text>
    </xsl:template>

    <!-- PROJECTION -->
    <xsl:template match="text()" mode="project"/>

    <xsl:template name="project">
        <xsl:param name="src"/>
        <xsl:param name="indent"/>

        <xsl:variable name="qname" select="concat(@scope,':',@name)"/>

        <xsl:value-of select="$indent"/>
        <xsl:text>for $n in </xsl:text>
        <xsl:value-of select="$src"/>
        <xsl:text> return</xsl:text>
        <xsl:value-of select="$NL"/>

        <xsl:value-of select="concat($indent,$indentStep)"/>
        <xsl:text>&lt;</xsl:text>
        <xsl:value-of select="$qname"/>
        <xsl:text>&gt;{</xsl:text>
        <xsl:value-of select="$NL"/>

        <!-- replace @xml:id by @iddf:id, as the projection might be redundant -->
        <xsl:value-of select="concat($indent,$indentStep,$indentStep)"/>
        <xsl:text>attribute iddf:id {$n/@xml:id},</xsl:text>
        <xsl:value-of select="$NL"/>
        <xsl:value-of select="concat($indent,$indentStep,$indentStep)"/>
        <xsl:text>$n/@* except $n/@xml:id,</xsl:text>
        <xsl:value-of select="$NL"/>

        <!-- add @iddf:type -->
        <xsl:value-of select="concat($indent,$indentStep,$indentStep)"/>
        <xsl:text>attribute iddf:type { id($n/@iddf:notion,$doc)/@type },</xsl:text>
        <xsl:value-of select="$NL"/>

        <!-- add labels (when requested) -->
        <xsl:value-of select="concat($indent,$indentStep,$indentStep)"/>
        <xsl:text>iddf-library:notion-label($n/@iddf:notion,$labels),</xsl:text>
        <xsl:value-of select="$NL"/>
        <xsl:value-of select="concat($indent,$indentStep,$indentStep)"/>
        <xsl:text>iddf-library:key-label($n/@iddf:notion,$n/@key,$labels),</xsl:text>
        <xsl:value-of select="$NL"/>

        <!-- add IDDF annotations and values -->
        <xsl:value-of select="concat($indent,$indentStep,$indentStep)"/>
        <xsl:text>iddf-library:annotations($n/iddf:annotation,$labels),</xsl:text>
        <xsl:value-of select="$NL"/>
        <xsl:value-of select="concat($indent,$indentStep,$indentStep)"/>
        <xsl:text>iddf-library:values($n/iddf:value,$labels),</xsl:text>
        <xsl:value-of select="$NL"/>

        <!-- add other IDDF nodes -->
        <xsl:value-of select="concat($indent,$indentStep,$indentStep)"/>
        <xsl:text>$n/(iddf:* except (iddf:value, iddf:annotation))</xsl:text>

        <!-- more projections -->
        <xsl:if test="exists(*)">
            <xsl:text>,</xsl:text>
            <xsl:value-of select="$NL"/>
            <xsl:for-each select="*">
                <xsl:apply-templates select="." mode="project">
                    <xsl:with-param name="src">
                        <xsl:text>$n/</xsl:text>
                        <xsl:value-of select="concat(@scope,':',@name)"/>
                    </xsl:with-param>
                    <xsl:with-param name="indent" select="concat($indent,$indentStep,$indentStep)"
                        tunnel="yes"/>
                </xsl:apply-templates>
                <xsl:if test="position()!=last()">
                    <xsl:text>,</xsl:text>
                    <xsl:value-of select="$NL"/>
                </xsl:if>
            </xsl:for-each>
        </xsl:if>
        <xsl:value-of select="$NL"/>

        <xsl:value-of select="concat($indent,$indentStep)"/>
        <xsl:text>}&lt;/</xsl:text>
        <xsl:value-of select="$qname"/>
        <xsl:text>&gt;</xsl:text>
    </xsl:template>

    <!-- nested root notion needs to be dereferenced -->
    <xsl:template match="iddf:notion[@type='root'][exists(ancestor::iddf:notion)]" mode="project">
        <xsl:param name="src"/>
        <xsl:param name="indent" tunnel="yes"/>
        <xsl:call-template name="project">
            <xsl:with-param name="src">
                <xsl:text>id(</xsl:text>
                <xsl:value-of select="$src"/>
                <xsl:text>/@iddf:ref,$doc)</xsl:text>
            </xsl:with-param>
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:call-template>
    </xsl:template>

    <xsl:template match="*" mode="project">
        <xsl:param name="src"/>
        <xsl:param name="indent" tunnel="yes"/>
        <xsl:call-template name="project">
            <xsl:with-param name="src" select="$src"/>
            <xsl:with-param name="indent" select="$indent"/>
        </xsl:call-template>
    </xsl:template>

    <!-- MAIN -->
    <xsl:template match="/iddq:query">

        <!-- declare the namespaces -->
        <xsl:text>declare namespace iddf-ws = 'http://languagelink.let.uu.nl/tds/ns/iddf-ws';</xsl:text>
        <xsl:value-of select="$NL"/>
        <xsl:text>declare namespace iddf = 'http://languagelink.let.uu.nl/tds/ns/iddf';</xsl:text>
        <xsl:value-of select="$NL"/>
        <xsl:value-of select="$NL"/>
        <xsl:text>import module namespace iddf-library = "http://exist.dans.knaw.nl/exist/iddf-library" at "iddf-library.xqm" ;</xsl:text>
        <xsl:value-of select="$NL"/>
        <xsl:value-of select="$NL"/>

        <xsl:for-each-group select=".//iddf:notion" group-by="@scope">
            <xsl:text>declare namespace </xsl:text>
            <xsl:value-of select="current-grouping-key()"/>
            <xsl:text> = '</xsl:text>
            <xsl:value-of select="id(current-grouping-key())/@ns"/>
            <xsl:text>';</xsl:text>
            <xsl:value-of select="$NL"/>
        </xsl:for-each-group>
        <xsl:value-of select="$NL"/>

        <!-- declare the labels flag -->
        <xsl:text>(:</xsl:text>
        <xsl:text>declare variable $labels := </xsl:text>
        <xsl:choose>
            <xsl:when test="$labels='true'">
                <xsl:text>true()</xsl:text>
            </xsl:when>
            <xsl:otherwise>
                <xsl:text>false()</xsl:text>
            </xsl:otherwise>
        </xsl:choose>
        <xsl:text>;</xsl:text>
        <xsl:text>:)</xsl:text>
        <xsl:value-of select="$NL"/>
        <xsl:value-of select="$NL"/>

        <!-- declare the IDDF doc -->
        <xsl:text>declare variable $doc := doc('</xsl:text>
        <xsl:value-of select="$file"/>
        <xsl:text>');</xsl:text>
        <xsl:value-of select="$NL"/>
        <xsl:value-of select="$NL"/>

        <!-- select the roots -->
        <xsl:for-each-group select="iddf:notion[@type='root']" group-by="@id">
            <xsl:text>declare variable $roots_</xsl:text>
            <xsl:value-of select="current-grouping-key()"/>
            <xsl:text> := $doc</xsl:text>
            <xsl:value-of select="iddq:select(current-grouping-key())"/>
            <xsl:text>;</xsl:text>
            <xsl:value-of select="$NL"/>
        </xsl:for-each-group>

        <xsl:value-of select="$NL"/>
        <xsl:text>&lt;iddf-ws:reply>{</xsl:text>
        <xsl:value-of select="$NL"/>
        <xsl:value-of select="$NL"/>

        <!-- project the roots -->
        <xsl:for-each select="iddf:notion[@type='root'](:[exists(iddq:project)]:)">
            <xsl:variable name="root">
                <xsl:copy>
                    <xsl:copy-of select="@*"/>
                    <xsl:copy-of select="iddq:project/*"/>
                </xsl:copy>
            </xsl:variable>
            <xsl:apply-templates select="$root" mode="project">
                <xsl:with-param name="src">
                    <xsl:text>$roots_</xsl:text>
                    <xsl:value-of select="@id"/>
                </xsl:with-param>
                <xsl:with-param name="indent" select="$indentStep" tunnel="yes"/>
            </xsl:apply-templates>
        </xsl:for-each>

        <xsl:value-of select="$NL"/>
        <xsl:value-of select="$NL"/>
        <xsl:text>}&lt;/iddf-ws:reply></xsl:text>
        <xsl:value-of select="$NL"/>
    </xsl:template>

</xsl:stylesheet>
