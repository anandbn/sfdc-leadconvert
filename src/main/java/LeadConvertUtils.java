import java.util.*;
import com.sforce.soap.partner.DeleteResult;
import com.sforce.soap.partner.LeadConvert;
import com.sforce.soap.partner.LeadConvertResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.sobject.SObject;
import com.sforce.ws.ConnectionException;

public class LeadConvertUtils {
	//All Leads modified by Ryan Huber
	private static String LEAD_QUERY=	"select Id,Email,Company,Heroku_User_Id__c from Lead "+
															 "where LastModifiedById='005300000055YGT'"+
															 "and Status!= 'Unqualified' "+
															 "and Status !='Prospect - Disqualified'"+
															 "and Heroku_User_Id__c !=null"+
												              "           and IsConverted=false";
	private static String CONTACT_QUERY=	"select Id,Heroku_User_Email__c from Contact "+
																	"where Heroku_User_Id__c in (%s)";

	private static String ACCOUNT_QUERY=	"select Id,Name from Account where Name in (%s)";

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		PartnerConnection pconn = ConnectionUtils.getPartnerConnection(System.getenv("SFDC_USERNAME"),
																										System.getenv("SFDC_PASSWORD"), 
																										"https://login.salesforce.com/services/Soap/u/24.0");
		com.sforce.soap.partner.QueryResult result = pconn.query(String.format(LEAD_QUERY));
		
		String userIdsToFetch = "",accountNames="";;
		SObject[] matchingLeads = result.getRecords();
		if(result.getRecords().length >0){
			for(SObject lead:matchingLeads){
				if(lead.getField("heroku_user_id__c")!=null)
					userIdsToFetch += String.format("'%s',",lead.getField("heroku_user_id__c"));
				if(lead.getField("company")!=null && !((String)lead.getField("company")).equalsIgnoreCase("[not provided]"))
					accountNames +=String.format("'%s',",lead.getField("company"));
			}
			userIdsToFetch = userIdsToFetch.substring(0,userIdsToFetch.length()-1);
			if(accountNames.length()>0)
				accountNames = accountNames.substring(0,accountNames.length()-1);
		}
		result = pconn.query(String.format(CONTACT_QUERY,userIdsToFetch));
		String[] contactsToDelete = new String[200];
		List<String> toDelete = new ArrayList<String>();
		if(result.getRecords().length >0){
			

			for(SObject contact:result.getRecords()){
				if(toDelete.size()==50){
					System.out.println(">>>>>>>>>>>>> Before deleting final list of contacts:"+contactsToDelete.length);
					deleteContacts(pconn, toDelete.toArray(new String[toDelete.size()]));
					
					toDelete = new ArrayList<String>();
					System.out.println(">>>>>>>>>>>>> Deleted Contacts");
				}
				System.out.println(String.format(">>>>>>>>>>>>> Contact to be Deleted(Id=%s",contact.getId()));
				toDelete.add(contact.getId());
			}
		}
		System.out.println(">>>>>>>>>>>>> Before deleting final list of contacts:"+contactsToDelete.length);
		deleteContacts(pconn,  toDelete.toArray(new String[toDelete.size()]));
		
		System.out.println(">>>>>>>>>>>>> Deleted Contacts");
		
		
		Map<String,String> accountMap = new HashMap<String,String>();
		if(accountNames.length()>0){
		result = pconn.query(String.format(ACCOUNT_QUERY,accountNames));
		if(result.getRecords().length >0){
			for(SObject account:result.getRecords()){
				System.out.println(String.format(">>>>>>>>>>>>> Account(Id=%s,Name%s)",account.getField("name"),account.getId()));
				accountMap.put((String)account.getField("name"), account.getId());
			}
		}
		}
		
		List<LeadConvert> toConvert = new ArrayList<LeadConvert>();
		LeadConvert lc;
		if(matchingLeads.length >0){
			for(SObject lead:matchingLeads){
				if(toConvert.size()==20){
					convertLeads(pconn,toConvert.toArray(new LeadConvert[toConvert.size()]));
					toConvert = new ArrayList<LeadConvert>();
				}
				lc = new LeadConvert();
		        lc.setLeadId(lead.getId());
		        lc.setOwnerId("0053000000693m8");
		        
		        lc.setConvertedStatus("User - Engaged");
		        if(accountMap.containsKey((String)lead.getField("company"))){
		            lc.setAccountId(accountMap.get((String)lead.getField("company")));
		        }else{
		            lc.setAccountId("00130000014LJB7");
		        }
		        lc.setDoNotCreateOpportunity(true);
		        lc.setSendNotificationEmail(false);
		       
		        toConvert.add(lc);
			}
		
		}
		convertLeads(pconn,toConvert.toArray(new LeadConvert[toConvert.size()]));
		
	}
	
	private static void deleteContacts(PartnerConnection pconn,String[] contactsToDelete) throws ConnectionException{
		DeleteResult[] deleteResults = pconn.delete(contactsToDelete);
		for (int i = 0; i < deleteResults.length; i++) {
		      DeleteResult deleteResult = deleteResults[i];
		      if (deleteResult.isSuccess()) {
		        System.out.println("Deleted Contact ID: " + 
		            deleteResult.getId()
		        );
		      } else {
		        // Handle the errors.
		        // We just print the first error out for sample purposes.
		        com.sforce.soap.partner.Error[] errors = deleteResult.getErrors();
		        if (errors.length > 0)  {
		          System.out.println("Error: could not delete  ID =" + deleteResult.getId());
		          System.out.println("   The error reported was: (" +
		              errors[0].getStatusCode() + ") " +
		              errors[0].getMessage() + "\n"
		          );
		        }
		      }
		    }
				
	}
	
	private static void convertLeads(PartnerConnection pconn,LeadConvert[] toConvert) throws ConnectionException{
		LeadConvertResult[] lcResults =  pconn.convertLead(toConvert);
	    for (int j = 0; j < lcResults.length; ++j) {
	      if (lcResults[j].isSuccess()) {
	        System.out.println(String.format("LeadId=%s,Contact Id=%s,AccountId%s",lcResults[j].getLeadId(),lcResults[j].getContactId(),lcResults[j].getAccountId()));
	      } else {
	        System.out.println("\nError converting new Lead: " +
	            lcResults[j].getErrors()[0].getMessage());
	      }
	    }
	}

}
