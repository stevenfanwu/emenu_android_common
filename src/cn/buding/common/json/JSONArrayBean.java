package cn.buding.common.json;


public interface JSONArrayBean<T extends JSONBean> extends JSONBean {
	public boolean add(T t);

	public T get(int index);
	
	public Class<T> getGenericClass();
}
