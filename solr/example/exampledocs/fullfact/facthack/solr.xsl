<?xml version="1.0"?>

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

<xsl:template match="/">
<haystack version="2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="http://fullfact.org/static/schema/haystack.xsd">
<xsl:attribute name="batch">cmhansard/scrapedxml/debates/<xsl:value-of select="substring(//speech[1]/@id, 26, 11)" /></xsl:attribute>
<xsl:attribute name="id">cmhansard:<xsl:value-of select="substring(//speech[1]/@id, 26, 11)" /></xsl:attribute>
  <xsl:attribute name="latest"><xsl:apply-templates select="publicwhip/@latest" /></xsl:attribute>
  <!-- Ignore paras with no speaker (the Report describing action) -->
  <xsl:apply-templates select="//speech[not(@nospeaker)]" />
</haystack>
</xsl:template>

<xsl:template match="speech">
<meta>
  <xsl:attribute name="authorname"><xsl:apply-templates select="@speakername" /></xsl:attribute>
  <xsl:attribute name="author"><xsl:apply-templates select="@person_id" /></xsl:attribute>
  <xsl:attribute name="publication">cmhansard</xsl:attribute>
  <xsl:attribute name="pdate"><xsl:value-of select="substring(@id, 26, 10)" />T<xsl:apply-templates select="@time" />Z</xsl:attribute>
  <xsl:attribute name="url"><xsl:apply-templates select="@url" /></xsl:attribute>
  <xsl:apply-templates select="p" />
</meta>
</xsl:template>


<xsl:template match="@time">
  <xsl:choose>
    <xsl:when test="string-length(.) = 9">
      <xsl:value-of select="substring(., 2)" />
    </xsl:when>
    <xsl:otherwise>
      <xsl:value-of select="." />
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

<xsl:template match="p">
  <xsl:apply-templates /><xsl:text>

</xsl:text>
</xsl:template>

</xsl:stylesheet>
