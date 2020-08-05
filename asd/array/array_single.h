#pragma once

#include "array_multi.h"

using namespace std;


template <typename T>
class array_single
{
public:
    int length;
    T *elems;

    array_single()
    {
        length = 0;
        elems = nullptr;
    }

    array_single(int size)
    {
        if (size < 0)
        {
            throw std::runtime_error("negative array size");
        }
        else if (size == 0)
        {
            length = 0;
            elems = nullptr;
        }
        else
        {
            length = size;
            elems = new T[length];
            for (size_t i = 0; i < length; i++)
            {
                elems[i] = 0;
            }
        }
    }

  
    array_single(int *sizes, int n)
    {
        if(n>1){
            throw "error: single dim expected";
        }
        length = sizes[0];
            elems = new T[length];
            for (size_t i = 0; i < length; i++)
            {
                elems[i] = 0;
            }
    }

    array_single(std::initializer_list<T> list)
    {
        length = list.size();
        elems = new T[length];
        std::copy(list.begin(), list.end(), elems);
    }

    T &operator[](int index) const
    {
        if (index < length && index >= 0)
        {
            return elems[index];
        }
        throw std::runtime_error(this->format("array index out of bounds exception: index=%d size=%d", index, length));
    }

    array_single<T> operator=(std::initializer_list<T> rhs)
    {
        length = rhs.size();
        elems = new T[length];
        std::copy(rhs.begin(), rhs.end(), elems);
        return this;
    }

    template <typename... Args>
     std::string format(const std::string &format, Args... args) const
        {
            size_t size = snprintf(nullptr, 0, format.c_str(), args...) + 1; // Extra space for '\0'
            std::unique_ptr<char[]> buf(new char[size]);
            snprintf(buf.get(), size, format.c_str(), args...);
            return std::string(buf.get(), buf.get() + size - 1); // We don't want the '\0' inside
        }
};
