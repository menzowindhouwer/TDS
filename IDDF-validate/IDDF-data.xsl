<?xml version="1.0"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:rng="http://relaxng.org/ns/structure/1.0" xmlns:iddf="http://languagelink.let.uu.nl/tds/ns/iddf" xmlns:a="http://relaxng.org/ns/compatibility/annotations/1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:sch="http://purl.oclc.org/dsdl/schematron">

	<xsl:output method="xml" encoding="utf-8"/>

	<xsl:param name="ns-base" select="concat('http://languagelink.let.uu.nl/tds/ns/tmp/',generate-id(/*),'/')"/>

	<xsl:variable name="IDDF_NS" select="'http://languagelink.let.uu.nl/tds/ns/iddf'"/>
	<xsl:variable name="enums" select="false()"/>
	
	<xsl:template match="text()"/>

	<xsl:template match="/iddf:iddf/iddf:documentation">
		<rng:grammar ns="{$IDDF_NS}" datatypeLibrary="http://www.w3.org/2001/XMLSchema-datatypes">
			
			<sch:ns uri="http://languagelink.let.uu.nl/tds/ns/iddf" prefix="iddf"/>
			
			<sch:pattern name="the data source should be known">
				<sch:rule context="iddf:data/*">
					<sch:assert test="exists(@iddf:src) or exists(@iddf:srcs) or exists(parent::iddf:data/@src)">The data source of a root notion instance should be specified.</sch:assert>
				</sch:rule>
			</sch:pattern>

			<sch:pattern name="the set of data sources can only shrink">
				<sch:rule context="*[exists(@iddf:src) or exists(@iddf:srcs)]">
					<sch:let name="o-srcs" value="ancestor::*[exists(@iddf:src) or exists(@iddf:srcs) or self::iddf:data/@src][1]/(@iddf:src,tokenize(@iddf:srcs,'\s+'),self::iddf:data/@src)"/>
					<sch:let name="inner-srcs" value="(@iddf:src,tokenize(@iddf:srcs,'\s+'))"/>
					<sch:let name="outer-srcs" value="if (exists($o-srcs)) then ($o-srcs) else (if (exists(/iddf:iddf/iddf:data/@src)) then (/iddf:iddf/iddf:data/@src) else (if (exists(parent::iddf:data)) then ($inner-srcs) else ()))"/>
					<sch:let name="intersect" value="distinct-values($inner-srcs[.=$outer-srcs])"/>
					<sch:let name="except" value="distinct-values($inner-srcs[not(.=$intersect)])"/>
					<sch:assert test="empty($except)">An notion instance data source (<sch:value-of select="string-join($except,', ')"/>) must also be the data source of the parent notion instance (<sch:value-of select="string-join($outer-srcs,', ')"/>).</sch:assert>
				</sch:rule>
			</sch:pattern>
			
			<sch:pattern name="check the key reference">
				<sch:rule context="*[exists(@iddf:key)]">
					<sch:assert test="exists(id(@iddf:key)/self::iddf:key[exists(parent::iddf:keys[exists(parent::iddf:notion)])])">The key value id should refer to a key value in a notion key value enumeration</sch:assert>
				</sch:rule>
			</sch:pattern>

			<rng:start>
				<rng:choice>
					<!-- list all root notions -->
					<xsl:choose>
						<xsl:when test="/iddf:iddf/iddf:data/@src">
							<xsl:variable name="scopes" select="id(/iddf:iddf/iddf:data/@src)/ancestor-or-self::iddf:scope/@xml:id"/>
							<xsl:for-each select="iddf:notion[@type='root'][@scope=$scopes]">
								<xsl:choose>
									<xsl:when test="@xml:id">
										<rng:ref name="notion_{@xml:id}"/>
									</xsl:when>
									<xsl:otherwise>
										<rng:ref name="notion_{generate-id(.)}"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						</xsl:when>
						<xsl:otherwise>
							<xsl:for-each select="iddf:notion[@type='root']">
								<xsl:choose>
									<xsl:when test="@xml:id">
										<rng:ref name="notion_{@xml:id}"/>
									</xsl:when>
									<xsl:otherwise>
										<rng:ref name="notion_{generate-id(.)}"/>
									</xsl:otherwise>
								</xsl:choose>
							</xsl:for-each>
						</xsl:otherwise>
					</xsl:choose>
				</rng:choice>
			</rng:start>
			
			<rng:define name="IDDF_data_value">
				<!-- TODO: should become a template so we can enumerate values at specific locations -->
				<rng:element name="value" ns="{$IDDF_NS}">
					<rng:optional>
						<rng:attribute name="xml:id">
							<rng:data type="ID"/>
						</rng:attribute>
					</rng:optional>
					<rng:optional>
						<rng:attribute name="val">
							<!--<rng:data type="IDREF"/>-->
							<text/>
						</rng:attribute>
						<sch:pattern name="check the value reference">
							<sch:rule context="iddf:value[exists(@val)]">
								<sch:assert test="exists(id(@val)/self::iddf:value[exists(parent::iddf:values[exists(parent::iddf:notion)])])">The value id should refer to a value in a notion value enumeration</sch:assert>
							</sch:rule>
						</sch:pattern>
						<sch:pattern name="check the notion value reference">
							<sch:rule context="*[exists(@iddf:notion)]/iddf:value[exists(@val)]">
								<sch:let name="notion" value="parent::*/@iddf:notion"/>
								<sch:assert test="id(@val)/parent::iddf:values/parent::iddf:notion/@xml:id=$notion">The value id should refer to a value in the containing notions value enumeration</sch:assert>
							</sch:rule>
						</sch:pattern>
					</rng:optional>
					<rng:optional>
						<rng:attribute name="datatype">
							<!--<rng:data type="IDREFS"/>-->
							<text/>
						</rng:attribute>
						<sch:pattern name="check the type reference">
							<sch:rule context="*[exists(@iddf:notion)]/iddf:value[exists(@datatype)]">
								<sch:let name="notion-type" value="id(../@iddf:notion)/self::iddf:notion/iddf:values/@datatype"/>
								<sch:let name="last-type" value="tokenize(@datatype,'\s+')[last()]"/>
								<sch:assert test="(normalize-space($notion-type) eq '') or ($notion-type eq $last-type)">The value type should be the same as the notion value data type</sch:assert>
							</sch:rule>
						</sch:pattern>
						<sch:pattern name="check the full type hierarchy">
							<sch:rule context="iddf:value[exists(@datatype)]">
								<sch:let name="last-type" value="tokenize(@datatype,'\s+')[last()]"/>
								<sch:let name="all-types" value="id($last-type)/self::iddf:datatype/ancestor-or-self::iddf:datatype/@xml:id"/>
								<sch:assert test="(@datatype eq $last-type) or (@datatype eq string-join($all-types,' '))">The value type should refer to exact data type or the full data type hierarchy</sch:assert>
							</sch:rule>
						</sch:pattern>
					</rng:optional>
					<rng:optional>
						<rng:choice>
							<rng:ref name="IDDF_val_srcs"/>
							<rng:ref name="IDDF_val_src"/>
						</rng:choice>
						<sch:pattern name="the set of data sources can only shrink">
							<sch:rule context="iddf:value[exists(@src) or exists(@srcs)]">
								<sch:let name="outer-srcs" value="ancestor::*[exists(@iddf:src) or exists(@iddf:srcs)][1]/(@iddf:src,tokenize(@iddf:srcs,'\s+'))"/>
								<sch:let name="inner-srcs" value="(@src,tokenize(@srcs,'\s+'))"/>
								<sch:let name="intersect" value="distinct-values($inner-srcs[.=$outer-srcs])"/>
								<sch:let name="except" value="distinct-values($inner-srcs[not(.=$intersect)])"/>
								<sch:assert test="empty($except)">A value data source (<sch:value-of select="string-join($except,', ')"/>) must also be the data source of the notion (<sch:value-of select="string-join($outer-srcs,', ')"/>).</sch:assert>
							</sch:rule>
						</sch:pattern>
					</rng:optional>
					<rng:optional>
						<!-- attribute to group a value and its annotations together -->
						<rng:attribute name="ann"/>
						<sch:pattern name="unique value annotation group">
							<sch:rule context="iddf:value[exists(@ann)]">
								<sch:let name="ann" value="@ann"/>
								<sch:assert test="empty((preceding-sibling::iddf:value|following-sibling::iddf:value)[@ann=$ann])">A value annotation group should be locally unique.</sch:assert>
							</sch:rule>
						</sch:pattern>
					</rng:optional>
					<rng:choice>
						<rng:group>
							<rng:attribute name="nil" ns="http://www.w3.org/2001/XMLSchema-instance">
								<rng:value>true</rng:value>
							</rng:attribute>
							<rng:empty/>
						</rng:group>
						<rng:group>
							<rng:optional>
								<rng:attribute name="nil" ns="http://www.w3.org/2001/XMLSchema-instance" a:defaultValue="false">
									<rng:value>false</rng:value>
								</rng:attribute>
							</rng:optional>
							<rng:choice>
								<rng:text/>
								<rng:ref name="any"/>
							</rng:choice>
						</rng:group>
					</rng:choice>
				</rng:element>
			</rng:define>

			<xsl:if test="/iddf:iddf/iddf:documentation/iddf:annotation">
				<rng:define name="IDDF_data_value_annotation">
					<rng:element name="annotation" ns="{$IDDF_NS}">
						<rng:optional>
							<rng:attribute name="xml:id">
								<rng:data type="ID"/>
							</rng:attribute>
						</rng:optional>
						<rng:optional>
							<rng:attribute name="val">
								<!--<rng:data type="IDREF"/>-->
								<text/>
							</rng:attribute>
							<sch:pattern name="check the annotation value reference">
								<sch:rule context="iddf:annotation[exists(@val)]">
									<sch:let name="ann" value="@type"/>
									<sch:assert test="id(@val)/parent::iddf:values/parent::iddf:annotation/@xml:id=$ann">The value id should refer to a value in the annotations value enumeration</sch:assert>
								</sch:rule>
							</sch:pattern>
						</rng:optional>
						<rng:optional>
							<rng:choice>
								<rng:ref name="IDDF_val_srcs"/>
								<rng:ref name="IDDF_val_src"/>
							</rng:choice>
							<sch:pattern name="the set of data sources can only shrink">
								<sch:rule context="iddf:annotation[exists(@src) or exists(@srcs)]">
									<sch:let name="ann" value="@ann"/>
									<sch:let name="val-srcs" value="(preceding-sibling::iddf:value|following-sibling::iddf:value)[@ann=$ann]/(@src,tokenize(@srcs,'\s+'))"/>
									<sch:let name="ann-srcs" value="(@src,tokenize(@srcs,'\s+'))"/>
									<sch:let name="intersect" value="distinct-values($ann-srcs[.=$val-srcs])"/>
									<sch:let name="except" value="distinct-values($ann-srcs[not(.=$intersect)])"/>
									<sch:assert test="empty($except)">An annotation data source (<sch:value-of select="string-join($except,', ')"/>) must also be the data source of the value (<sch:value-of select="string-join($val-srcs,', ')"/>).</sch:assert>
								</sch:rule>
							</sch:pattern>
						</rng:optional>
						<!-- attribute to group a value and its annotations together -->
						<rng:attribute name="ann"/>
						<sch:pattern name="unique value annotation group">
							<sch:rule context="iddf:data//iddf:annotation">
								<sch:let name="ann" value="@ann"/>
								<sch:assert test="count((preceding-sibling::iddf:value|following-sibling::iddf:value)[@ann=$ann])=1">An value annotation should be about one value.</sch:assert>
							</sch:rule>
						</sch:pattern>
						<rng:choice>
							<xsl:for-each select="/iddf:iddf/iddf:documentation//iddf:annotation">
								<rng:group>
									<rng:attribute name="type">
										<xsl:choose>
											<xsl:when test="parent::iddf:annotation">
												<rng:choice>
													<rng:value>
														<xsl:value-of select="@xml:id"/>
													</rng:value>
													<rng:value>
														<xsl:for-each select="ancestor-or-self::iddf:annotation">
															<xsl:value-of select="@xml:id"/>
															<xsl:if test="position()!=last()">
																<xsl:text> </xsl:text>
															</xsl:if>
														</xsl:for-each>
													</rng:value>
												</rng:choice>
											</xsl:when>
											<xsl:otherwise>
												<rng:value>
													<xsl:value-of select="@xml:id"/>
												</rng:value>
											</xsl:otherwise>
										</xsl:choose>
									</rng:attribute>
									<xsl:choose>
										<xsl:when test="iddf:values[@coverage='total']">
											<rng:choice>
												<xsl:for-each select="iddf:values/iddf:value/iddf:literal">
													<rng:value>
														<xsl:value-of select="."/>
													</rng:value>
												</xsl:for-each>
											</rng:choice>
										</xsl:when>
										<xsl:otherwise>
											<rng:choice>
												<rng:text/>
												<rng:ref name="any"/>
											</rng:choice>
										</xsl:otherwise>
									</xsl:choose>
								</rng:group>
							</xsl:for-each>
						</rng:choice>
					</rng:element>
				</rng:define>
			</xsl:if>

			<rng:define name="IDDF_src">
				<rng:attribute name="src" ns="{$IDDF_NS}">
					<xsl:choose>
						<xsl:when test="/iddf:iddf/iddf:data/@src">
							<xsl:attribute name="a:defaultValue">
								<xsl:value-of select="/iddf:iddf/iddf:data/@src"/>
							</xsl:attribute>
							<rng:value>
								<xsl:value-of select="/iddf:iddf/iddf:data/@src"/>
							</rng:value>
						</xsl:when>
						<xsl:otherwise>
							<rng:choice>
								<xsl:for-each select=".//iddf:scope[@type='datasource']">
									<rng:value>
										<xsl:value-of select="@xml:id"/>
									</rng:value>
								</xsl:for-each>
							</rng:choice>
						</xsl:otherwise>
					</xsl:choose>
				</rng:attribute>
			</rng:define>

			<rng:define name="IDDF_srcs">
				<rng:attribute name="srcs" ns="{$IDDF_NS}">
					<xsl:choose>
						<xsl:when test="/iddf:iddf/iddf:data/@src">
							<xsl:attribute name="a:defaultValue">
								<xsl:value-of select="/iddf:iddf/iddf:data/@src"/>
							</xsl:attribute>
							<rng:value>
								<xsl:value-of select="/iddf:iddf/iddf:data/@src"/>
							</rng:value>
						</xsl:when>
						<xsl:otherwise>
							<rng:list>
								<rng:oneOrMore>
									<rng:choice>
										<xsl:for-each select=".//iddf:scope[@type='datasource']">
											<rng:value>
												<xsl:value-of select="@xml:id"/>
											</rng:value>
										</xsl:for-each>
									</rng:choice>
								</rng:oneOrMore>
							</rng:list>
						</xsl:otherwise>
					</xsl:choose>
				</rng:attribute>
			</rng:define>

			<rng:define name="IDDF_val_src">
				<rng:attribute name="src">
					<xsl:choose>
						<xsl:when test="/iddf:iddf/iddf:data/@src">
							<xsl:attribute name="a:defaultValue">
								<xsl:value-of select="/iddf:iddf/iddf:data/@src"/>
							</xsl:attribute>
							<rng:value>
								<xsl:value-of select="/iddf:iddf/iddf:data/@src"/>
							</rng:value>
						</xsl:when>
						<xsl:otherwise>
							<rng:choice>
								<xsl:for-each select=".//iddf:scope[@type='datasource']">
									<rng:value>
										<xsl:value-of select="@xml:id"/>
									</rng:value>
								</xsl:for-each>
							</rng:choice>
						</xsl:otherwise>
					</xsl:choose>
				</rng:attribute>
			</rng:define>

			<rng:define name="IDDF_val_srcs">
				<rng:attribute name="srcs">
					<xsl:choose>
						<xsl:when test="/iddf:iddf/iddf:data/@src">
							<xsl:attribute name="a:defaultValue">
								<xsl:value-of select="/iddf:iddf/iddf:data/@src"/>
							</xsl:attribute>
							<rng:value>
								<xsl:value-of select="/iddf:iddf/iddf:data/@src"/>
							</rng:value>
						</xsl:when>
						<xsl:otherwise>
							<rng:list>
								<rng:oneOrMore>
									<rng:choice>
										<xsl:for-each select=".//iddf:scope[@type='datasource']">
											<rng:value>
												<xsl:value-of select="@xml:id"/>
											</rng:value>
										</xsl:for-each>
									</rng:choice>
								</rng:oneOrMore>
							</rng:list>
						</xsl:otherwise>
					</xsl:choose>
				</rng:attribute>
			</rng:define>
			
			<rng:define name="IDDF_ref">
				<rng:attribute name="iddf:ref">
					<rng:data type="IDREF"/>
				</rng:attribute>
			</rng:define>
			
			<rng:define name="any">
				<rng:zeroOrMore>
					<rng:choice>
						<rng:text/>
						<rng:element>
							<rng:anyName>
								<rng:except>
									<rng:nsName ns="http://languagelink.let.uu.nl/tds/ns/iddf"/>
								</rng:except>
							</rng:anyName>
							<rng:zeroOrMore>
								<rng:choice>
									<rng:attribute>
										<rng:anyName>
											<rng:except>
												<rng:nsName ns="http://languagelink.let.uu.nl/tds/ns/iddf"/>
												<rng:name ns="http://www.w3.org/XML/1998/namespace">id</rng:name>
											</rng:except>
										</rng:anyName>
									</rng:attribute>
								</rng:choice>
							</rng:zeroOrMore>
							<rng:ref name="any"/>
						</rng:element>
					</rng:choice>
				</rng:zeroOrMore>
			</rng:define>

			<xsl:apply-templates select="iddf:notion"/>

		</rng:grammar>
	</xsl:template>

	<xsl:template match="iddf:notion">
		<xsl:choose>
			<xsl:when test="/iddf:iddf/iddf:data/@src">
				<xsl:variable name="scopes" select="id(/iddf:iddf/iddf:data/@src)/ancestor-or-self::iddf:scope/@xml:id"/>
				<xsl:if test="@scope=$scopes">
					<xsl:choose>
						<xsl:when test="@xml:id">
							<rng:define name="notion_{@xml:id}">
								<xsl:call-template name="IDDF_notion"/>
							</rng:define>
						</xsl:when>
						<xsl:otherwise>
							<rng:define name="notion_{generate-id(.)}">
								<xsl:call-template name="IDDF_notion"/>
							</rng:define>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:apply-templates select="iddf:notion"/>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="@xml:id">
						<rng:define name="notion_{@xml:id}">
							<xsl:call-template name="IDDF_notion"/>
						</rng:define>
					</xsl:when>
					<xsl:otherwise>
						<rng:define name="notion_{generate-id(.)}">
							<xsl:call-template name="IDDF_notion"/>
						</rng:define>
					</xsl:otherwise>
				</xsl:choose>
				<xsl:apply-templates select="iddf:notion"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="iddf:notion/iddf:notion[@ref]" priority="1">
		<xsl:variable name="notion" select="id(current()/@ref)"/>
		<xsl:if test="$notion[@type='root'] and (count(preceding::iddf:notion[@ref=current()/@ref])=0)">
			<rng:define name="notion_{@ref}_ref">
				<rng:element name="{$notion/@name}" ns="{id($notion/@scope)/@ns}">
					<rng:optional>
						<rng:ref name="IDDF_ref"/>
					</rng:optional>
					<rng:optional>
						<rng:attribute name="notion" ns="{$IDDF_NS}" a:defaultValue="{@ref}">
							<rng:value>
								<xsl:value-of select="@ref"/>
							</rng:value>
						</rng:attribute>
					</rng:optional>
					<rng:optional>
						<rng:ref name="IDDF_srcs"/>
					</rng:optional>
					<rng:attribute name="ref">
						<xsl:if test="id(@ref)/iddf:keys/@coverage='total' and $enums">
							<rng:choice>
								<xsl:for-each select="id(@ref)/iddf:keys/iddf:key/iddf:literal">
									<rng:value>
										<xsl:value-of select="."/>
									</rng:value>
								</xsl:for-each>
							</rng:choice>
						</xsl:if>
					</rng:attribute>
				</rng:element>
			</rng:define>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="IDDF_notion">
		<xsl:variable name="ns">
			<xsl:choose>
				<xsl:when test="normalize-space(id(current()/@scope)/@ns)!=''">
					<xsl:value-of select="id(current()/@scope)/@ns"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="concat($ns-base,current()/@scope)"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<rng:element name="{@name}" ns="{$ns}">
			<rng:optional>
				<rng:attribute name="xml:id">
					<rng:data type="ID"/>
				</rng:attribute>
			</rng:optional>
			<xsl:if test="@xml:id">
				<rng:optional>
					<rng:attribute name="notion" ns="{$IDDF_NS}" a:defaultValue="{@xml:id}">
						<rng:value>
							<xsl:value-of select="@xml:id"/>
						</rng:value>
					</rng:attribute>
				</rng:optional>
			</xsl:if>
			<xsl:if test="@datatype">
				<rng:optional>
					<rng:attribute name="datatype" ns="{$IDDF_NS}" a:defaultValue="{@datatype}">
						<xsl:choose>
							<xsl:when test="id(@datatype)/self::iddf:datatype/parent::iddf:datatype">
								<rng:choice>
									<rng:value>
										<xsl:value-of select="@datatype"/>
									</rng:value>
									<rng:value>
										<xsl:for-each select="id(@datatype)/self::iddf:datatype/ancestor-or-self::iddf:datatype/@xml:id">
											<xsl:value-of select="."/>
											<xsl:if test="position()!=last()">
												<xsl:text> </xsl:text>
											</xsl:if>
										</xsl:for-each>
									</rng:value>
								</rng:choice>
							</xsl:when>
							<xsl:otherwise>
								<rng:value>
									<xsl:value-of select="@datatype"/>
								</rng:value>
							</xsl:otherwise>
						</xsl:choose>
					</rng:attribute>
				</rng:optional>
			</xsl:if>
			<rng:optional>
				<rng:attribute name="key">
					<xsl:if test="iddf:keys/@coverage='total' and $enums">
						<rng:choice>
							<xsl:for-each select="iddf:keys/iddf:key/iddf:literal">
								<rng:value>
									<xsl:value-of select="."/>
								</rng:value>
							</xsl:for-each>
						</rng:choice>
					</xsl:if>
				</rng:attribute>
			</rng:optional>
			<rng:optional>
				<rng:attribute name="iddf:key">
					<!--<rng:data type="IDREF"/>-->
				</rng:attribute>
			</rng:optional>
			<rng:choice>
				<rng:group>
					<rng:optional>
						<rng:choice>
							<rng:ref name="IDDF_srcs"/>
							<rng:ref name="IDDF_src"/>
						</rng:choice>
					</rng:optional>
					<rng:choice>
						<!-- one value as content of the notion -->
						<rng:text/>
						<xsl:if test="iddf:notion">
							<!-- child notions -->
							<xsl:choose>
								<xsl:when test="/iddf:iddf/iddf:data/@src">
									<xsl:variable name="scopes" select="id(/iddf:iddf/iddf:data/@src)/ancestor-or-self::iddf:scope/@xml:id"/>
									<xsl:if test="iddf:notion[@scope=$scopes or id(@ref)/@scope=$scopes]">
										<rng:interleave>
											<xsl:apply-templates select="iddf:notion[@scope=$scopes or id(@ref)/@scope=$scopes]" mode="ref"/>
										</rng:interleave>
									</xsl:if>
								</xsl:when>
								<xsl:otherwise>
									<rng:interleave>
										<xsl:apply-templates select="iddf:notion" mode="ref"/>
									</rng:interleave>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:if>
					</rng:choice>
				</rng:group>
				<rng:group>
					<!-- multiple iddf values and/or annotations -->
					<rng:optional>
						<rng:choice>
							<rng:ref name="IDDF_srcs"/>
							<rng:ref name="IDDF_src"/>
							<!-- TODO: could be more restrictive if we know which scopes are allowed to instantiate this notion -->
						</rng:choice>
					</rng:optional>
					<rng:interleave>
						<rng:group>
							<rng:optional>
								<xsl:choose>
									<xsl:when test="/iddf:iddf/iddf:documentation/iddf:annotation">
										<rng:ref name="IDDF_data_value"/>
										<rng:zeroOrMore>
											<rng:choice>
												<rng:ref name="IDDF_data_value_annotation"/>
												<rng:ref name="IDDF_data_value"/>
											</rng:choice>
										</rng:zeroOrMore>
									</xsl:when>
									<xsl:otherwise>
										<rng:oneOrMore>
											<rng:ref name="IDDF_data_value"/>
										</rng:oneOrMore>
									</xsl:otherwise>
								</xsl:choose>
							</rng:optional>
						</rng:group>
						<xsl:if test="iddf:notion">
							<xsl:choose>
								<xsl:when test="/iddf:iddf/iddf:data/@src">
									<xsl:variable name="scopes" select="id(/iddf:iddf/iddf:data/@src)/ancestor-or-self::iddf:scope/@xml:id"/>
									<xsl:apply-templates select="iddf:notion[@scope=$scopes]" mode="ref"/>
									<xsl:apply-templates select="iddf:notion[@ref][id(@ref)/@scope=$scopes]" mode="ref"/>
								</xsl:when>
								<xsl:otherwise>
									<xsl:apply-templates select="iddf:notion" mode="ref"/>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:if>
					</rng:interleave>
				</rng:group>
			</rng:choice>
		</rng:element>
	</xsl:template>

	<xsl:template match="iddf:notion/iddf:notion" mode="ref">
		<rng:zeroOrMore>
			<xsl:choose>
				<xsl:when test="@xml:id">
					<rng:ref name="notion_{@xml:id}"/>					
				</xsl:when>
				<xsl:otherwise>
					<rng:ref name="notion_{generate-id(.)}"/>
				</xsl:otherwise>
			</xsl:choose>
		</rng:zeroOrMore>
	</xsl:template>

	<xsl:template match="iddf:notion/iddf:notion[@ref]" mode="ref" priority="1">
		<rng:zeroOrMore>
			<xsl:variable name="notion" select="id(current()/@ref)"/>
			<xsl:choose>
				<xsl:when test="$notion[@type='root']">
					<rng:ref name="notion_{@ref}_ref"/>
				</xsl:when>
				<xsl:otherwise>
					<rng:ref name="notion_{@ref}"/>
				</xsl:otherwise>
			</xsl:choose>
		</rng:zeroOrMore>
	</xsl:template>

</xsl:stylesheet>
