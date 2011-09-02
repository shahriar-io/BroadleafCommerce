/*
 * Copyright 2008-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.broadleafcommerce.openadmin.server.service.persistence.module;

import com.anasoft.os.daofusion.criteria.AssociationPath;
import com.anasoft.os.daofusion.criteria.PersistentEntityCriteria;
import com.anasoft.os.daofusion.cto.client.CriteriaTransferObject;
import com.anasoft.os.daofusion.cto.client.FilterAndSortCriteria;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadleafcommerce.openadmin.client.dto.*;
import org.broadleafcommerce.openadmin.client.service.ServiceException;
import org.broadleafcommerce.openadmin.server.cto.BaseCtoConverter;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

/**
 * 
 * @author jfischer
 *
 */
public class JoinStructurePersistenceModule extends BasicPersistenceModule {
	
	private static final Log LOG = LogFactory.getLog(JoinStructurePersistenceModule.class);
	
	public boolean isCompatible(OperationType operationType) {
		return OperationType.JOINSTRUCTURE.equals(operationType);
	}
	
	public void extractProperties(Map<MergedPropertyType, Map<String, FieldMetadata>> mergedProperties, List<Property> properties) throws NumberFormatException {
		if (mergedProperties.get(MergedPropertyType.JOINSTRUCTURE) != null) {
			extractPropertiesFromMetadata(mergedProperties.get(MergedPropertyType.JOINSTRUCTURE), properties, true);
		}
	}

	protected BaseCtoConverter getJoinStructureCtoConverter(PersistencePerspective persistencePerspective, CriteriaTransferObject cto, Map<String, FieldMetadata> mergedProperties, JoinStructure joinStructure) throws ClassNotFoundException {
		BaseCtoConverter ctoConverter = getCtoConverter(persistencePerspective, cto, joinStructure.getJoinStructureEntityClassname(), mergedProperties);
		ctoConverter.addLongEQMapping(joinStructure.getJoinStructureEntityClassname(), joinStructure.getName(), AssociationPath.ROOT, joinStructure.getLinkedObjectPath() + "." + joinStructure.getLinkedIdProperty());
		ctoConverter.addLongEQMapping(joinStructure.getJoinStructureEntityClassname(), joinStructure.getName() + "Target", AssociationPath.ROOT, joinStructure.getTargetObjectPath() + "." + joinStructure.getTargetIdProperty());
		return ctoConverter;
	}
	
	protected Serializable createPopulatedJoinStructureInstance(JoinStructure joinStructure, Entity entity) throws InstantiationException, IllegalAccessException, ClassNotFoundException, NumberFormatException, InvocationTargetException, NoSuchMethodException {
		Serializable instance = (Serializable) Class.forName(joinStructure.getJoinStructureEntityClassname()).newInstance();
		String targetPath = joinStructure.getTargetObjectPath() + "." + joinStructure.getTargetIdProperty();
		String linkedPath = joinStructure.getLinkedObjectPath() + "." + joinStructure.getLinkedIdProperty();
		getFieldManager().setFieldValue(instance, linkedPath, Long.valueOf(entity.findProperty(linkedPath).getValue()));
		getFieldManager().setFieldValue(instance, targetPath, Long.valueOf(entity.findProperty(targetPath).getValue()));
		
		return instance;
	}
	
	@Override
	public void updateMergedProperties(PersistencePackage persistencePackage, Map<MergedPropertyType, Map<String, FieldMetadata>> allMergedProperties, Map<String, FieldMetadata> metadataOverrides) throws ServiceException {
		String ceilingEntityFullyQualifiedClassname = persistencePackage.getCeilingEntityFullyQualifiedClassname();
		try {
			PersistencePerspective persistencePerspective = persistencePackage.getPersistencePerspective();
			JoinStructure joinStructure = (JoinStructure) persistencePerspective.getPersistencePerspectiveItems().get(PersistencePerspectiveItemType.JOINSTRUCTURE);
			if (joinStructure != null) {
				Map<String, FieldMetadata> joinMergedProperties = persistenceManager.getDynamicEntityDao().getMergedProperties(
					joinStructure.getJoinStructureEntityClassname(), 
					new Class[]{Class.forName(joinStructure.getJoinStructureEntityClassname())}, 
					null, 
					new String[]{}, 
					new ForeignKey[]{},
					MergedPropertyType.JOINSTRUCTURE,
					persistencePerspective.getPopulateToOneFields(), 
					persistencePerspective.getIncludeFields(), 
					persistencePerspective.getExcludeFields(),
					metadataOverrides,
					""
				);
				allMergedProperties.put(MergedPropertyType.JOINSTRUCTURE, joinMergedProperties);
			}
		} catch (Exception e) {
			LOG.error("Problem fetching results for " + ceilingEntityFullyQualifiedClassname, e);
			throw new ServiceException("Unable to fetch results for " + ceilingEntityFullyQualifiedClassname, e);
		}
	}
	
	@Override
	public Entity add(PersistencePackage persistencePackage) throws ServiceException {
		String[] customCriteria = persistencePackage.getCustomCriteria();
		if (customCriteria != null && customCriteria.length > 0) {
			LOG.warn("custom persistence handlers and custom criteria not supported for add types other than ENTITY");
		}
		PersistencePerspective persistencePerspective = persistencePackage.getPersistencePerspective();
		String ceilingEntityFullyQualifiedClassname = persistencePackage.getCeilingEntityFullyQualifiedClassname();
		Entity entity = persistencePackage.getEntity();
		JoinStructure joinStructure = (JoinStructure) persistencePerspective.getPersistencePerspectiveItems().get(PersistencePerspectiveItemType.JOINSTRUCTURE);
		Entity payload;
		try {
			Class<?>[] entities = persistenceManager.getPolymorphicEntities(ceilingEntityFullyQualifiedClassname);
			Map<String, FieldMetadata> mergedPropertiesTarget = persistenceManager.getDynamicEntityDao().getMergedProperties(
				ceilingEntityFullyQualifiedClassname, 
				entities, 
				null, 
				persistencePerspective.getAdditionalNonPersistentProperties(), 
				persistencePerspective.getAdditionalForeignKeys(),
				MergedPropertyType.PRIMARY,
				persistencePerspective.getPopulateToOneFields(), 
				persistencePerspective.getIncludeFields(), 
				persistencePerspective.getExcludeFields(),
				null,
				""
			);
			Map<String, FieldMetadata> mergedProperties = persistenceManager.getDynamicEntityDao().getMergedProperties(
				joinStructure.getJoinStructureEntityClassname(), 
				new Class[]{Class.forName(joinStructure.getJoinStructureEntityClassname())}, 
				null, 
				new String[]{}, 
				new ForeignKey[]{},
				MergedPropertyType.JOINSTRUCTURE,
				false,
				new String[]{},
				new String[]{},
				null,
				""
			);
			
			CriteriaTransferObject ctoInserted = new CriteriaTransferObject();
			FilterAndSortCriteria filterCriteriaInsertedLinked = ctoInserted.get(joinStructure.getName());
			String linkedPath;
			String targetPath;
			if (joinStructure.getInverse()) {
				linkedPath = joinStructure.getTargetObjectPath() + "." + joinStructure.getTargetIdProperty();
				targetPath = joinStructure.getLinkedObjectPath() + "." + joinStructure.getLinkedIdProperty();
			} else {
				targetPath = joinStructure.getTargetObjectPath() + "." + joinStructure.getTargetIdProperty();
				linkedPath = joinStructure.getLinkedObjectPath() + "." + joinStructure.getLinkedIdProperty();
			}
			filterCriteriaInsertedLinked.setFilterValue(entity.findProperty(joinStructure.getInverse()?targetPath:linkedPath).getValue());
			FilterAndSortCriteria filterCriteriaInsertedTarget = ctoInserted.get(joinStructure.getName()+"Target");
			filterCriteriaInsertedTarget.setFilterValue(entity.findProperty(joinStructure.getInverse()?linkedPath:targetPath).getValue());
			BaseCtoConverter ctoConverterInserted = getJoinStructureCtoConverter(persistencePerspective, ctoInserted, mergedProperties, joinStructure);
			PersistentEntityCriteria queryCriteriaInserted = ctoConverterInserted.convert(ctoInserted, joinStructure.getJoinStructureEntityClassname());
			List<Serializable> recordsInserted = persistenceManager.getDynamicEntityDao().query(queryCriteriaInserted, Class.forName(joinStructure.getJoinStructureEntityClassname()));
			if (recordsInserted.size() > 0) {
				payload = getRecords(mergedPropertiesTarget, recordsInserted, mergedProperties, joinStructure.getTargetObjectPath())[0];
			} else {
				Serializable instance = createPopulatedJoinStructureInstance(joinStructure, entity);
				instance = createPopulatedInstance(instance, entity, mergedProperties, false);
				instance = createPopulatedInstance(instance, entity, mergedPropertiesTarget, false);
				FieldManager fieldManager = getFieldManager();
				if (fieldManager.getField(instance.getClass(), "id") != null) {
					fieldManager.setFieldValue(instance, "id", null);
				}
				if (joinStructure.getSortField() != null) {
					CriteriaTransferObject cto = new CriteriaTransferObject();
					FilterAndSortCriteria filterCriteria = cto.get(joinStructure.getName());
					filterCriteria.setFilterValue(entity.findProperty(joinStructure.getInverse()?targetPath:linkedPath).getValue());
					FilterAndSortCriteria sortCriteria = cto.get(joinStructure.getSortField());
					sortCriteria.setSortAscending(joinStructure.getSortAscending());
					BaseCtoConverter ctoConverter = getJoinStructureCtoConverter(persistencePerspective, cto, mergedProperties, joinStructure);
					int totalRecords = getTotalRecords(joinStructure.getJoinStructureEntityClassname(), cto, ctoConverter);
					fieldManager.setFieldValue(instance, joinStructure.getSortField(), Long.valueOf(totalRecords + 1));
				}
				instance = persistenceManager.getDynamicEntityDao().merge(instance);
				persistenceManager.getDynamicEntityDao().flush();
				persistenceManager.getDynamicEntityDao().clear();
				
				List<Serializable> recordsInserted2 = persistenceManager.getDynamicEntityDao().query(queryCriteriaInserted, Class.forName(joinStructure.getJoinStructureEntityClassname()));
				
				payload = getRecords(mergedPropertiesTarget, recordsInserted2, mergedProperties, joinStructure.getTargetObjectPath())[0];
			}
		} catch (Exception e) {
			LOG.error("Problem editing entity", e);
			throw new ServiceException("Problem adding new entity : " + e.getMessage(), e);
		}
		
		return payload;
	}
	
	@Override
	public Entity update(PersistencePackage persistencePackage) throws ServiceException {
		String[] customCriteria = persistencePackage.getCustomCriteria();
		if (customCriteria != null && customCriteria.length > 0) {
			LOG.warn("custom persistence handlers and custom criteria not supported for update types other than ENTITY");
		}
		PersistencePerspective persistencePerspective = persistencePackage.getPersistencePerspective();
		Entity entity = persistencePackage.getEntity();
		JoinStructure joinStructure = (JoinStructure) persistencePerspective.getPersistencePerspectiveItems().get(PersistencePerspectiveItemType.JOINSTRUCTURE);
		try {
			CriteriaTransferObject cto = new CriteriaTransferObject();
			FilterAndSortCriteria filterCriteria = cto.get(joinStructure.getName());
			filterCriteria.setFilterValue(entity.findProperty(joinStructure.getLinkedObjectPath() + "." + joinStructure.getLinkedIdProperty()).getValue());
			if (joinStructure.getSortField() != null) {
				FilterAndSortCriteria sortCriteria = cto.get(joinStructure.getSortField());
				sortCriteria.setSortAscending(joinStructure.getSortAscending());
			}
			
			Map<String, FieldMetadata> mergedProperties = persistenceManager.getDynamicEntityDao().getMergedProperties(
				joinStructure.getJoinStructureEntityClassname(), 
				new Class[]{Class.forName(joinStructure.getJoinStructureEntityClassname())}, 
				null, 
				new String[]{}, 
				new ForeignKey[]{},
				MergedPropertyType.JOINSTRUCTURE,
				persistencePerspective.getPopulateToOneFields(), 
				persistencePerspective.getIncludeFields(), 
				persistencePerspective.getExcludeFields(),
				null,
				""
			);
			BaseCtoConverter ctoConverter = getJoinStructureCtoConverter(persistencePerspective, cto, mergedProperties, joinStructure);
			PersistentEntityCriteria queryCriteria = ctoConverter.convert(cto, joinStructure.getJoinStructureEntityClassname());
			List<Serializable> records = persistenceManager.getDynamicEntityDao().query(queryCriteria, Class.forName(joinStructure.getJoinStructureEntityClassname()));
			
			int index = 0;
			Long myEntityId = Long.valueOf(entity.findProperty(joinStructure.getTargetObjectPath() + "." + joinStructure.getTargetIdProperty()).getValue());	
			FieldManager fieldManager = getFieldManager();
			for (Serializable record : records) {
				Long targetId = (Long) fieldManager.getFieldValue(record, joinStructure.getTargetObjectPath() + "." + joinStructure.getTargetIdProperty());
				if (myEntityId.equals(targetId)) {
					break;
				}
				index++;
			}
			if (joinStructure.getSortField() != null && entity.findProperty(joinStructure.getSortField()).getValue() != null) {
				Serializable myRecord = records.remove(index);
				myRecord = createPopulatedInstance(myRecord, entity, mergedProperties, false);
				Integer newPos = Integer.valueOf(entity.findProperty(joinStructure.getSortField()).getValue());
				records.add(newPos, myRecord);
				index = 1;
				for (Serializable record : records) {
					fieldManager.setFieldValue(record, joinStructure.getSortField(), Long.valueOf(index));
					index++;
				}
			} else {
				Serializable myRecord = records.get(index);
				myRecord = createPopulatedInstance(myRecord, entity, mergedProperties, false);
				persistenceManager.getDynamicEntityDao().merge(myRecord);
			}
			
			return entity;
		} catch (Exception e) {
			LOG.error("Problem editing entity", e);
			throw new ServiceException("Problem updating entity : " + e.getMessage(), e);
		}
	}
	
	@Override
	public void remove(PersistencePackage persistencePackage) throws ServiceException {
		String[] customCriteria = persistencePackage.getCustomCriteria();
		if (customCriteria != null && customCriteria.length > 0) {
			LOG.warn("custom persistence handlers and custom criteria not supported for remove types other than ENTITY");
		}
		PersistencePerspective persistencePerspective = persistencePackage.getPersistencePerspective();
		Entity entity = persistencePackage.getEntity();
		try {
			JoinStructure joinStructure = (JoinStructure) persistencePerspective.getPersistencePerspectiveItems().get(PersistencePerspectiveItemType.JOINSTRUCTURE);
			Map<String, FieldMetadata> mergedProperties = persistenceManager.getDynamicEntityDao().getMergedProperties(
				joinStructure.getJoinStructureEntityClassname(), 
				new Class[]{Class.forName(joinStructure.getJoinStructureEntityClassname())}, 
				null, 
				new String[]{}, 
				new ForeignKey[]{},
				MergedPropertyType.JOINSTRUCTURE,
				false,
				new String[]{},
				new String[]{},
				null,
				""
			);
			CriteriaTransferObject ctoInserted = new CriteriaTransferObject();
			FilterAndSortCriteria filterCriteriaInsertedLinked = ctoInserted.get(joinStructure.getName());
			filterCriteriaInsertedLinked.setFilterValue(entity.findProperty(joinStructure.getLinkedObjectPath() + "." + joinStructure.getLinkedIdProperty()).getValue());
			FilterAndSortCriteria filterCriteriaInsertedTarget = ctoInserted.get(joinStructure.getName()+"Target");
			filterCriteriaInsertedTarget.setFilterValue(entity.findProperty(joinStructure.getTargetObjectPath() + "." + joinStructure.getTargetIdProperty()).getValue());
			BaseCtoConverter ctoConverterInserted = getJoinStructureCtoConverter(persistencePerspective, ctoInserted, mergedProperties, joinStructure);
			PersistentEntityCriteria queryCriteriaInserted = ctoConverterInserted.convert(ctoInserted, joinStructure.getJoinStructureEntityClassname());
			List<Serializable> recordsInserted = persistenceManager.getDynamicEntityDao().query(queryCriteriaInserted, Class.forName(joinStructure.getJoinStructureEntityClassname()));
			
			persistenceManager.getDynamicEntityDao().remove(recordsInserted.get(0));
		} catch (Exception e) {
			LOG.error("Problem removing entity", e);
			throw new ServiceException("Problem removing entity : " + e.getMessage(), e);
		}
	}
	
	@Override
	public DynamicResultSet fetch(PersistencePackage persistencePackage, CriteriaTransferObject cto) throws ServiceException {
		PersistencePerspective persistencePerspective = persistencePackage.getPersistencePerspective();
		String ceilingEntityFullyQualifiedClassname = persistencePackage.getCeilingEntityFullyQualifiedClassname();
		JoinStructure joinStructure = (JoinStructure) persistencePerspective.getPersistencePerspectiveItems().get(PersistencePerspectiveItemType.JOINSTRUCTURE);
		Entity[] payload;
		int totalRecords;
		try {
			Class<?>[] entities = persistenceManager.getPolymorphicEntities(ceilingEntityFullyQualifiedClassname);
			Map<String, FieldMetadata> mergedPropertiesTarget = persistenceManager.getDynamicEntityDao().getMergedProperties(
				ceilingEntityFullyQualifiedClassname, 
				entities, 
				null, 
				persistencePerspective.getAdditionalNonPersistentProperties(), 
				persistencePerspective.getAdditionalForeignKeys(),
				MergedPropertyType.PRIMARY,
				persistencePerspective.getPopulateToOneFields(), 
				persistencePerspective.getIncludeFields(), 
				persistencePerspective.getExcludeFields(),
				null,
				""
			);
			Map<String, FieldMetadata> mergedProperties = persistenceManager.getDynamicEntityDao().getMergedProperties(
				joinStructure.getJoinStructureEntityClassname(), 
				new Class[]{Class.forName(joinStructure.getJoinStructureEntityClassname())}, 
				null, 
				new String[]{}, 
				new ForeignKey[]{},
				MergedPropertyType.JOINSTRUCTURE,
				false,
				new String[]{},
				new String[]{},
				null,
				""
			);
			BaseCtoConverter ctoConverter = getJoinStructureCtoConverter(persistencePerspective, cto, mergedProperties, joinStructure);
			PersistentEntityCriteria queryCriteria = ctoConverter.convert(cto, joinStructure.getJoinStructureEntityClassname());
			List<Serializable> records = persistenceManager.getDynamicEntityDao().query(queryCriteria, Class.forName(joinStructure.getJoinStructureEntityClassname()));
			payload = getRecords(mergedPropertiesTarget, records, mergedProperties, joinStructure.getTargetObjectPath());
			totalRecords = getTotalRecords(joinStructure.getJoinStructureEntityClassname(), cto, ctoConverter);
		} catch (Exception e) {
			LOG.error("Problem fetching results for " + joinStructure.getJoinStructureEntityClassname(), e);
			throw new ServiceException("Unable to fetch results for " + joinStructure.getJoinStructureEntityClassname(), e);
		}
		
		DynamicResultSet results = new DynamicResultSet(null, payload, totalRecords);
		
		return results;
	}
}
