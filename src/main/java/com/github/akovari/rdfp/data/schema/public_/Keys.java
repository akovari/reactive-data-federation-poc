/**
 * This class is generated by jOOQ
 */
package com.github.akovari.rdfp.data.schema.public_;


import com.github.akovari.rdfp.data.schema.public_.tables.CaseLinks;
import com.github.akovari.rdfp.data.schema.public_.tables.records.CaseLinksRecord;

import javax.annotation.Generated;

import org.jooq.Identity;
import org.jooq.UniqueKey;
import org.jooq.impl.AbstractKeys;


/**
 * A class modelling foreign key relationships between tables of the <code>public</code> 
 * schema
 */
@Generated(
	value = {
		"http://www.jooq.org",
		"jOOQ version:3.7.0"
	},
	comments = "This class is generated by jOOQ"
)
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Keys {

	// -------------------------------------------------------------------------
	// IDENTITY definitions
	// -------------------------------------------------------------------------

	public static final Identity<CaseLinksRecord, Integer> IDENTITY_CASE_LINKS = Identities0.IDENTITY_CASE_LINKS;

	// -------------------------------------------------------------------------
	// UNIQUE and PRIMARY KEY definitions
	// -------------------------------------------------------------------------

	public static final UniqueKey<CaseLinksRecord> CASE_LINKS_PKEY = UniqueKeys0.CASE_LINKS_PKEY;

	// -------------------------------------------------------------------------
	// FOREIGN KEY definitions
	// -------------------------------------------------------------------------


	// -------------------------------------------------------------------------
	// [#1459] distribute members to avoid static initialisers > 64kb
	// -------------------------------------------------------------------------

	private static class Identities0 extends AbstractKeys {
		public static Identity<CaseLinksRecord, Integer> IDENTITY_CASE_LINKS = createIdentity(CaseLinks.CASE_LINKS, CaseLinks.CASE_LINKS.ID);
	}

	private static class UniqueKeys0 extends AbstractKeys {
		public static final UniqueKey<CaseLinksRecord> CASE_LINKS_PKEY = createUniqueKey(CaseLinks.CASE_LINKS, CaseLinks.CASE_LINKS.ID);
	}
}
