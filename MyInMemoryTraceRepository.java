package com.yeshj.soa.reservation.api.common.springbootactuator;

import org.springframework.boot.actuate.trace.Trace;
import org.springframework.boot.actuate.trace.TraceRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * copy InMemoryTraceRepository
 *
 * @author wufanyang@hujiang.com
 * @date 2018/6/8 10:24
 **/
@Component("MyInMemoryTraceRepository")
public class MyInMemoryTraceRepository implements TraceRepository {
    private int capacity = 100;
    private boolean reverse = true;
    private final List<Trace> traces = new LinkedList();

    public MyInMemoryTraceRepository() {
    }

    public void setReverse(boolean reverse) {
        List var2 = this.traces;
        synchronized(this.traces) {
            this.reverse = reverse;
        }
    }

    public void setCapacity(int capacity) {
        List var2 = this.traces;
        synchronized(this.traces) {
            this.capacity = capacity;
        }
    }

    public List<Trace> findAll() {
        List var1 = this.traces;
        synchronized(this.traces) {
            return Collections.unmodifiableList(new ArrayList(this.traces));
        }
    }

    public void add(Map<String, Object> map) {
        Trace trace = new Trace(new Date(), map);
        List var3 = this.traces;
        synchronized(this.traces) {
            while(this.traces.size() >= this.capacity) {
                this.traces.remove(this.reverse ? this.capacity - 1 : 0);
            }

            if (this.reverse) {
                this.traces.add(0, trace);
            } else {
                this.traces.add(trace);
            }

        }
    }
}