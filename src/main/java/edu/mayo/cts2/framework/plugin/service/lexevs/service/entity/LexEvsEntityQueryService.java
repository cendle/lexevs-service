/*
* Copyright: (c) 2004-2013 Mayo Foundation for Medical Education and
* Research (MFMER). All rights reserved. MAYO, MAYO CLINIC, and the
* triple-shield Mayo logo are trademarks and service marks of MFMER.
*
* Except as contained in the copyright notice above, or as used to identify
* MFMER as the author of this software, the trade names, trademarks, service
* marks, or product names of the copyright holder shall not be used in
* advertising, promotion or otherwise in connection with this software without
* prior written authorization of the copyright holder.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package edu.mayo.cts2.framework.plugin.service.lexevs.service.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.LexGrid.LexBIG.DataModel.Collections.LocalNameList;
import org.LexGrid.LexBIG.DataModel.Collections.ResolvedConceptReferenceList;
import org.LexGrid.LexBIG.DataModel.Collections.SortOptionList;
import org.LexGrid.LexBIG.DataModel.Core.CodingSchemeVersionOrTag;
import org.LexGrid.LexBIG.DataModel.Core.ResolvedConceptReference;
import org.LexGrid.LexBIG.DataModel.InterfaceElements.ExtensionDescription;
import org.LexGrid.LexBIG.LexBIGService.CodedNodeSet;
import org.LexGrid.LexBIG.LexBIGService.CodedNodeSet.PropertyType;
import org.LexGrid.LexBIG.LexBIGService.CodedNodeSet.SearchDesignationOption;
import org.LexGrid.LexBIG.LexBIGService.LexBIGService;
import org.LexGrid.LexBIG.Utility.Iterators.ResolvedConceptReferencesIterator;
import org.LexGrid.concepts.Entity;
import org.LexGrid.concepts.Presentation;
import org.springframework.stereotype.Component;

import edu.mayo.cts2.framework.model.command.Page;
import edu.mayo.cts2.framework.model.command.ResolvedFilter;
import edu.mayo.cts2.framework.model.command.ResolvedReadContext;
import edu.mayo.cts2.framework.model.core.EntityReferenceList;
import edu.mayo.cts2.framework.model.core.MatchAlgorithmReference;
import edu.mayo.cts2.framework.model.core.OpaqueData;
import edu.mayo.cts2.framework.model.core.PredicateReference;
import edu.mayo.cts2.framework.model.core.PropertyReference;
import edu.mayo.cts2.framework.model.core.SortCriteria;
import edu.mayo.cts2.framework.model.core.SourceReference;
import edu.mayo.cts2.framework.model.core.VersionTagReference;
import edu.mayo.cts2.framework.model.directory.DirectoryResult;
import edu.mayo.cts2.framework.model.entity.EntityDescription;
import edu.mayo.cts2.framework.model.entity.EntityDirectoryEntry;
import edu.mayo.cts2.framework.model.service.core.DocumentedNamespaceReference;
import edu.mayo.cts2.framework.model.service.core.EntityNameOrURI;
import edu.mayo.cts2.framework.model.service.core.EntityNameOrURIList;
import edu.mayo.cts2.framework.model.service.core.Query;
import edu.mayo.cts2.framework.plugin.service.lexevs.service.AbstractLexEvsService;
import edu.mayo.cts2.framework.plugin.service.lexevs.utility.PrintUtility;
import edu.mayo.cts2.framework.service.meta.StandardModelAttributeReference;
import edu.mayo.cts2.framework.service.profile.entitydescription.EntitiesFromAssociationsQuery;
import edu.mayo.cts2.framework.service.profile.entitydescription.EntitiesFromAssociationsQuery.EntitiesFromAssociations;
import edu.mayo.cts2.framework.service.profile.entitydescription.EntityDescriptionQuery;
import edu.mayo.cts2.framework.service.profile.entitydescription.EntityDescriptionQueryService;

@Component
public class LexEvsEntityQueryService extends AbstractLexEvsService 
		implements EntityDescriptionQueryService {

	// ------ Local methods ----------------------
	private EntityTransform entityTransform = new EntityTransform();

	public EntityTransform getEntityTransformer() {
		return entityTransform;
	}

	public void setEntityTransformer(
			EntityTransform entityTransform) {
		this.entityTransform = entityTransform;
	}

	
	// -------- Implemented methods ----------------
	@Override
	public int count(EntityDescriptionQuery query) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DirectoryResult<EntityDescription> getResourceList(
			EntityDescriptionQuery query, SortCriteria sortCriteria, Page page) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DirectoryResult<EntityDirectoryEntry> getResourceSummaries(
			EntityDescriptionQuery query, SortCriteria sortCriteria, Page page) {
		
		LexBIGService lexBigService = getLexBigService();
		ArrayList<EntityDirectoryEntry> list = new ArrayList<EntityDirectoryEntry>();
		DirectoryResult<EntityDirectoryEntry> directoryResult = null;
		boolean atEnd = false;
		
		try {		
			// Get codingScheme name
			String codingScheme = query.getRestrictions().getCodeSystemVersion().getName();			
			
			// Get Code Node Set from LexBIG service for given coding scheme
			CodingSchemeVersionOrTag versionOrTag = null;
			LocalNameList entityTypes = new LocalNameList();
			CodedNodeSet codeNodeSet = lexBigService.getNodeSet(codingScheme, versionOrTag, entityTypes);
			
			// Get resolved filter set from query argument
			Set<ResolvedFilter> filterSet = query.getFilterComponent();
			
			if(filterSet != null){
				// Restrict code node set using each filter in set
				for(ResolvedFilter filter : filterSet){
					String matchText = filter.getMatchValue();										// Value to search with 
					SearchDesignationOption option = SearchDesignationOption.ALL;					// Other options: PREFERRED_ONLY, NON_PREFERRED_ONLY, ALL 
					String matchAlgorithm = filter.getMatchAlgorithmReference().getContent();		// Extract from filter the match algorithm to use
					String language = null;															// This field is not really used, uses default "en"
					
					codeNodeSet.restrictToMatchingDesignations(matchText, option, matchAlgorithm, language);
				}
			}
			
			
			SortOptionList sortOptions = null;
			LocalNameList propertyNames = null;
			PropertyType [] propertyTypes = null; 
			// With all null arguments the iterator will access the entire codeNodeSet
			// This call will execute the set of filters determined in loop above
			ResolvedConceptReferencesIterator iterator = codeNodeSet.resolve(sortOptions, propertyNames, propertyTypes);
			
			// Get on requested "page" of entities.  
			// In this case we can get the "page" from the iterator, unlike in LexEvsCodeSystemVersionQueryService.
			int start = page.getStart();
			int end = page.getEnd();
			ResolvedConceptReferenceList resolvedConceptReferenceList = iterator.get(start, end);
			
			
//			System.out.println("resolvedConcpetReferenceList: " + resolvedConceptReferenceList.getResolvedConceptReferenceCount());
			
			// Get array of resolved concept references
			ResolvedConceptReference[] resolvedConceptReferences = resolvedConceptReferenceList.getResolvedConceptReference();
			System.out.println("resolvedConceptReferences: " + resolvedConceptReferences.length);
			
			// Transform each reference into a CTS2 entry and add to list
			for(ResolvedConceptReference reference : resolvedConceptReferences){
				System.out.println("ResolvedConceptReference:\n" + PrintUtility.resolvedConceptReference_toString(reference, 1));
				EntityDirectoryEntry entry = entityTransform.transform(reference);
				list.add(entry);
			}

			// Determine if this is the last "page"
			if(!iterator.hasNext()){ 
				atEnd = true;
			}
			
			directoryResult = new DirectoryResult<EntityDirectoryEntry>(list, atEnd);
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		return directoryResult;
	}
	
	@Override
	public Set<? extends MatchAlgorithmReference> getSupportedMatchAlgorithms() {
		for(ExtensionDescription desc :
			this.getLexBigService().getFilterExtensions().getExtensionDescription()){
			
		}
		
		return null;
	}

	@Override
	public Set<? extends PropertyReference> getSupportedSearchReferences() {
		PropertyReference ref = StandardModelAttributeReference.RESOURCE_SYNOPSIS.getPropertyReference();
		// presentation
		// resource_name - code (restricted code method)
		return new HashSet<PropertyReference>(Arrays.asList(ref)); 
	}

	@Override
	public Set<? extends PropertyReference> getSupportedSortReferences() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<PredicateReference> getKnownProperties() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServiceName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OpaqueData getServiceDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getServiceVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SourceReference getServiceProvider() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<DocumentedNamespaceReference> getKnownNamespaceList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEntityInSet(EntityNameOrURI entity, Query query,
			Set<ResolvedFilter> filterComponent,
			EntityDescriptionQuery restrictions, ResolvedReadContext readContext) {
		// TODO Auto-generated method stub
		return false;
	}



	@Override
	public EntityReferenceList resolveAsEntityReferenceList(
			EntityDescriptionQuery restrictions, ResolvedReadContext readContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EntityNameOrURIList intersectEntityList(
			Set<EntityNameOrURI> entities, EntityDescriptionQuery restrictions,
			ResolvedReadContext readContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<? extends VersionTagReference> getSupportedTags() {
		// TODO Auto-generated method stub
		return null;
	}

}
