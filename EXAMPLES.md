# Examples of usage


#### Simple use in tests
```
import io.bugreaper.modules.minio.Minio;

public class MyTests {
    Minio minio = new Minio(host, port, username, password)
    
    ..use base methods from Minio as is (minio.XXX)
}  
```

#### Create object with extend:
```
import io.bugreaper.modules.minio.Minio;

public class MinioSetup extends Minio {

    public MinioSetup() {
        super(host, port, username, password);
    }
    
    ..use base methods from Minio as is (minio.XXX)
}  
```

#### Create object with secondary methods:
```
import io.bugreaper.modules.minio.Minio;

public class MinioSetup {

    private final Minio minio;
    private final String TEST_BUCKET = "my-bucket";


    private MinioSetup() {
        this.minio = new Minio(host, port, username, password);
    }

    public void cleanMyBucket() {
        minio.cleanBucket(TEST_BUCKET);
    }

    public void uploadFileToMyBucket(String filePath) {
        minio.uploadFileToBucket(TEST_BUCKET, filePath);
    }
    
    ...other methods with your queue
}    
```

#### Create object with base methods & Singleton:
```
//TODO

import io.bugreaper.modules.minio.Minio;

    private static MinioSetup instance;
    private final Minio minio;


    public  MinioSetup() {
        this.minio = new Minio(host, port, username, password);
    }

    public static MinioSetup getInstance() {
        if (instance == null) {
            instance = new MinioSetup();
        }

        return instance;
    }

    ..use base methods from Minio as is
}  
```

#### Setup example:
```
//if use Singleton
protected final Minio minio = MinioSetup.getInstance(); 

//if use like object
protected final Minio minio = new MinioSetup();
```


#### Test examples with upload & download:
```
import static io.bugreaper.core.filereaders.ResourcesFileReader.*;

private static final String DEFAULT_BUCKET = "bucket-for-file";

@Test
void test() {

    var file = "saved_new_file_txt";

    minio.createBucket(DEFAULT_BUCKET);
    
    
    minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "object.txt");
    minio.downloadObjectFromBucket(DEFAULT_BUCKET, "object.txt", file);          
    
    // ResourcesFileReader from core
    
    seeResourceFileNotEmpty(file); 
    
    assertEquals(readResourceFile(file), readResourceFile(TEST_FILE_LINES2), "File read content successful");      

}
```


#### Test examples with asserts:
```
private static final String DEFAULT_BUCKET = "new-bucket";

@Test
void test() {

    minio.cleanBucket(DEFAULT_BUCKET);
    
    minio.createBucket(DEFAULT_BUCKET);
    minio.uploadFileToBucket(DEFAULT_BUCKET, "test_file_1.txt");
    
    minio.seeBucketIsNotEmpty(DEFAULT_BUCKET);                                 
    minio.seeObjectExists(DEFAULT_BUCKET, "test_file_1.txt");                 
    minio.seeCountObjectsInBucketExactly(DEFAULT_BUCKET, 1);                      
    minio.seeCountObjectsInBucketGreater(DEFAULT_BUCKET, 0);                      
    minio.seeCountObjectsInBucketLess(DEFAULT_BUCKET, 3);                      

}
```


#### Test examples with grab messages from queue:
- Assert all list of messages (until at least one matches)
- [AssertableStringList](https://ambu550.gitlab.io/java-bugreaper-core/apidocs/io/bugreaper/core/assertable/stringlist/ListOperators.html) checks:
- customMatcher for [Strings type](https://hamcrest.org/JavaHamcrest/javadoc/3.0/org/hamcrest/Matchers.html)
- recommended to clean queue before each test and work with one message (but you can work with as many as needed)
```

private static final String DEFAULT_BUCKET = "new-bucket";
private static final String TEST_FILE = "data/test_file_1.txt";


@Test
void test() {
                     
    minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "object1.txt");
    minio.uploadFileToBucket(DEFAULT_BUCKET, TEST_FILE, "object1.txt");

                     
    // list can be asserted by chain
    
    minio.getObjectsList(DEFAULT_BUCKET)
        .testInLIst(elementsCountInList(2))
        .testInLIst(stringEqualsInList("object1.txt"))
        .testInLIst(stringContainsInList("object2"));    
}
```

### [ALL interactions](https://ambu550.gitlab.io/java-bugreaper-module-minio/io/bugreaper/modules/minio/interfaces/MinioInt.html)


### Real examples here:
- [Report] - in progress
- [Tests] - in progress