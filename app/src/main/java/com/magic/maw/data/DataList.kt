package com.magic.maw.data

class DataList<K, T : IKey<K>> : ArrayList<T>() {
    private val map = LinkedHashMap<K, T>()

    override fun add(element: T): Boolean {
        map[element.key] = element
        return super.add(element)
    }

    override fun add(index: Int, element: T) {
        map[element.key] = element
        super.add(index, element)
    }

    override fun addAll(elements: Collection<T>): Boolean {
        for (data in elements)
            map[data.key] = data
        return super.addAll(elements)
    }

    override fun addAll(index: Int, elements: Collection<T>): Boolean {
        for (data in elements)
            map[data.key] = data
        return super.addAll(index, elements)
    }

    override fun remove(element: T): Boolean {
        map.remove(element.key)
        return super.remove(element)
    }

    override fun removeAll(elements: Collection<T>): Boolean {
        for (data in elements)
            map.remove(data.key)
        return super.removeAll(elements.toSet())
    }

    override fun removeAt(index: Int): T {
        map.remove(get(index).key)
        return super.removeAt(index)
    }

    override fun removeRange(fromIndex: Int, toIndex: Int) {
        for (index in fromIndex until toIndex) {
            map.remove(get(index).key)
        }
        super.removeRange(fromIndex, toIndex)
    }

    override fun clear() {
        map.clear()
        super.clear()
    }

    fun getData(index: Int): T? {
        if (size > index) {
            return get(index)
        }
        return null
    }

    fun getIndex(key: K): Int? {
        if (map.containsKey(key)) {
            for ((index, value) in this.withIndex()) {
                if (value.key == key) {
                    return index
                }
            }
        }
        return null
    }

    fun checkDataList(checkList: List<T>): List<T> {
        if (isEmpty())
            return checkList
        val newList = ArrayList<T>()
        for (item in checkList) {
            if (!map.containsKey(item.key)) {
                newList.add(item)
            }
        }
        return newList
    }
}