package core;

/**
 * Base task metadata model extending BaseEntity.
 * Demonstrates: Single Inheritance, encapsulation, keyword usage ('super' and 'this').
 */
public class BaseTask extends BaseEntity {
    private String taskName;
    private String description;
    private String category;
    private String userTag;
    private volatile String state;
    private long startTime;
    private long endTime;
    private long duration;

    public BaseTask(String taskName) {
        super();
        this.taskName = (taskName == null || taskName.trim().isEmpty()) ? "Unnamed-Task" : taskName;
        this.description = "No description provided";
        this.category = "General";
        this.userTag = "System";
        this.state = TaskConstants.STATE_CREATED;
        this.startTime = 0;
        this.endTime = 0;
        this.duration = 0;
    }

    public synchronized String getTaskName() { 
        return taskName; 
    }
    
    public synchronized void setTaskName(String taskName) { 
        this.taskName = (taskName == null || taskName.trim().isEmpty()) ? "Unnamed-Task" : taskName; 
    }

    public synchronized String getDescription() { 
        return description; 
    }

    public synchronized void setDescription(String description) { 
        this.description = (description == null || description.trim().isEmpty()) ? "No description provided" : description; 
    }

    public synchronized String getCategory() { 
        return category; 
    }

    public synchronized void setCategory(String category) { 
        this.category = (category == null || category.trim().isEmpty()) ? "General" : category; 
    }

    public synchronized String getUserTag() { 
        return userTag; 
    }

    public synchronized void setUserTag(String userTag) { 
        this.userTag = (userTag == null || userTag.trim().isEmpty()) ? "System" : userTag; 
    }

    public String getState() { 
        return state; 
    }

    public void setState(String state) { 
        this.state = state; 
    }

    public synchronized long getStartTime() { 
        return startTime; 
    }

    public synchronized void setStartTime(long startTime) { 
        this.startTime = startTime; 
    }

    public synchronized long getEndTime() { 
        return endTime; 
    }

    public synchronized void setEndTime(long endTime) { 
        this.endTime = endTime; 
        if (this.startTime > 0 && this.endTime >= this.startTime) {
            this.duration = this.endTime - this.startTime;
        }
    }

    public synchronized long getDuration() { 
        return duration; 
    }

    public synchronized void setDuration(long duration) {
        this.duration = duration;
    }
}
